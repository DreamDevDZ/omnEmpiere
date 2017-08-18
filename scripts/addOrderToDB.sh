#!/usr/bin/php
<?php
    class addOrderToDB {
    
        public $message = array();
    
        public function __construct() {
            set_time_limit(0);       
            }
    

        private function readStompMessage() {
        
        global $message;
        
        $queue  = '/queue/SqlStatements';
        $uri = 'tcp://localhost:61613';
        $username = 'xxxxx';
        $passwd = 'xxxxx';
        
        

        try {
            $stompCon = new Stomp($uri,$username,$passwd);
            } catch(StompException $e) {
                die('Connection failed: ' . $e->getMessage());
                }
        $stompCon->subscribe($queue);
        $iterate=0;
        $msg = $stompCon->readFrame();
        while (!is_bool($msg)) {
	    // mark the message as received in the queue
            $stompCon->ack($msg);
            $temp = simplexml_load_string($msg->body);
            $json = json_encode($temp);
            $messagearray = json_decode($json,TRUE);
		file_put_contents('../var/postgres.log', print_r($messagearray,true),FILE_APPEND);
            $message[$iterate] = $messagearray[0];
		file_put_contents('../var/postgres.log', print_r($message[$iterate],true),FILE_APPEND);
	    $iterate++;
            $msg = $stompCon->readFrame();
            }    
     
    
        }
        
        private function writeDB() {
            
            global $message;

            $conn_string = "host=omnempiere.cvlniysmijeg.eu-west-1.rds.amazonaws.com port=5432 dbname=idempiere user=adempiere password=xxxxxxx";
            $dbconn = pg_connect($conn_string);
             if($dbconn != null) {
                foreach ($message as $sql) { 
		    if(!(pg_query($dbconn,$sql))) {
                           $errorlog = "Error: ".$sql;
                           file_put_contents('../var/postgres.log', print_r($errorlog, true),FILE_APPEND);
                        } else {
                    file_put_conents('../var/postgres.log', "New record created successfully",FILE_APPEND);
                        }
                    }
                }
            pg_close($dbconn);                
            }
        
        public function run() {
            $this->readStompMessage(); 
            $this->writeDB();
        }
        
    }
    
    $result = new addOrderToDB();
    $result->run();

?>
