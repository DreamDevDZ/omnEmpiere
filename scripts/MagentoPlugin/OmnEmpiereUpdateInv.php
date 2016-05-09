<?php

require_once 'app/Mage.php';

class OmnEmpiereUpdateInv {
    
    
    public $sku = array();
    public $quantity = array();
    
    
    public function __construct() {
        set_time_limit(0);       
    }
    
    
    
    private function readStompMessage () {
        
        global $sku;
        global $quantity;
        
        $queue  = '/queue/UpdateMage';
        $uri = 'tcp://Your ServiceMix URI here:61613'; //   Add your ServiceMIX URI
        $username = 'smx';
        $passwd = 'smx';

        try {
            $stompCon = new Stomp($uri,$username,$passwd);
            } catch(StompException $e) {
                die('Connection failed: ' . $e->getMessage());
                }
        $stompCon->subscribe($queue);

        $msg = $stompCon->readFrame();
  
        if ( $msg != null) {
         echo "Received array: "; 
            print_r($msg->body);
            $stompCon->ack($msg);
        } else {
            echo "Failed to receive a message\n";
            }
        $temp = simplexml_load_string($msg->body);
        print_r($temp);
    
        foreach ($temp->Product as $product) {
             $sku[]=$product->SKU;
             print_r($sku);
             $quantity[]=$product->QtyOnHand;
             print_r($quantity);
            }
        
    }

    
    private function updateMagentoInv() {
   

        global $sku;
        global $quantity;


        
//prepare Environment

        Mage::register('isSecureArea', true);       
        Mage::app()->setCurrentStore(Mage_Core_Model_App::ADMIN_STORE_ID);
        
        $product = Mage::getModel('catalog/product');
        $setid = Mage::getModel('catalog/product')->getDefaultAttributeSetId();


//  loop through SKU
        
        foreach($sku as $key => $sku_value) {
            echo " KEY:  ";
            print_r($key);
            echo "  SKU: ";
		    print_r((string) $sku_value);
            echo "  quantity  ";
            print_r((float) $quantity[$key]);    
                
            try{     
                    
                    $product = Mage::getModel('catalog/product')->loadByAttribute('sku',(string)$sku_value); 

                    if ( $product ) {

                        $productId = $product->getId();
                        echo "    Product ID:   ";
                        print_r($productId);
                        $stockItem = Mage::getModel('cataloginventory/stock_item')->loadByProduct($productId);
                        $stock_data= $stockItem-> getData();
                        echo "   stock data   ";
                        print_r($stock_data);
                        $stockItem->setData('qty',(float) $quantity[$key] );
                        $stockItem->save();
                        
                    }
 
            }catch(Exception $e){
                print_r($e->getMessage());                  
                //Mage::log($e->getMessage());
                 }
        }
        Mage::unregister('isSecureArea'); 
    }
    
   
    
    public function run() {
        
        
        
        $this->readStompMessage();
        $this->updateMagentoInv();
    }
    
}

$result = new OmnEmpiereUpdateInv();
$result->run();
?>
