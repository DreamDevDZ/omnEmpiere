#!/usr/bin/php
<?php

    class registerEbayService {
    
        private $devID;
	    private $appID;
	    private $certID;
	    private $serverUrl;
	    private $compatLevel;
	    private $siteID;
	    private $verb;
    
    
        public function __construct($developerID,$applicationID,$certificateID,$compatabilityLevel,$siteToUseID,$callName,$serverUrl) {
            //$this->requestToken = $userRequestToken;
		  $this->devID = $developerID;
		  $this->appID = $applicationID;
		  $this->certID = $certificateID;
		  $this->compatLevel = $compatabilityLevel;
		  $this->siteID = $siteToUseID;
		  $this->verb = $callName;
          $this->serverUrl = $serverUrl;	
            }
        
        public function sendHttpRequest($requestBody)  {
	
    
      
		//build eBay headers using variables passed via constructor
		$headers = $this->buildEbayHeaders();
		print_r($headers);
        
		//initialise a CURL session
		$connection = curl_init();
        
      
		//set the server we are using (could be Sandbox or Production server)
		curl_setopt($connection, CURLOPT_URL, $this->serverUrl);
		
		//stop CURL from verifying the peer certificate
		curl_setopt($connection, CURLOPT_SSL_VERIFYPEER, 0);
		curl_setopt($connection, CURLOPT_SSL_VERIFYHOST, 0);
		
		//set the headers using the array of headers
		curl_setopt($connection, CURLOPT_HTTPHEADER, $headers);
		
		//set method as POST
		curl_setopt($connection, CURLOPT_POST, 1);
		
		//set the XML body of the request
		curl_setopt($connection, CURLOPT_POSTFIELDS, $requestBody);
		
		//set it to return the transfer as a string from curl_exec
		curl_setopt($connection, CURLOPT_RETURNTRANSFER, 1);
        
		//Send the Request
		$response = curl_exec($connection);
        
        //print $response;
		
		//close the connection
		curl_close($connection);
		
		//return the response
		return $response;
	}
    
       private function buildEbayHeaders()
	{
		$headers = array (
			//Regulates versioning of the XML interface for the API
			'X-EBAY-API-COMPATIBILITY-LEVEL: ' . $this->compatLevel,
			
			//set the keys
			'X-EBAY-API-DEV-NAME: ' . $this->devID,
			'X-EBAY-API-APP-NAME: ' . $this->appID,
			'X-EBAY-API-CERT-NAME: ' . $this->certID,
			
			//the name of the call we are requesting
			'X-EBAY-API-CALL-NAME: ' . $this->verb,			
			
			//SiteID must also be set in the Request's XML
			//SiteID = 0  (US) - UK = 3, Canada = 2, Australia = 15, ....
			//SiteID Indicates the eBay site to associate the call with
			'X-EBAY-API-SITEID: ' . $this->siteID,
		);
		
		return $headers;
	}
    
            
}
    
        $developerID='a0243766-3f86-4f7f-bb0e-2dc447edff35';
	    $applicationID='AnubesIT-769a-4b06-ad61-5b898a6371eb';
	    $certificateID='5daee421-7829-462f-93e4-fc7e91d0d415';
	    $serverUrl='https://api.sandbox.ebay.com/ws/api.dll';
	    $compatabilityLevel='931';
	    $siteToUseID= '0';
	    $callName= 'SetNotificationPreferences';
    
    $xmlfile = file_get_contents('./ebayRegisterService.xml', FILE_USE_INCLUDE_PATH);
    
    $result = new registerEbayService($developerID,$applicationID,$certificateID,$compatabilityLevel,$siteToUseID,$callName,$serverUrl);
    $answer = $result->sendHttpRequest($xmlfile);
    print_r($answer);

?>