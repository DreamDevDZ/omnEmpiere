<?php
namespace OmnEmpiere\Tools\Model;

use \Symfony\Component\Console\Command\Command;
use \Symfony\Component\Console\Input\InputInterface;
use \Symfony\Component\Console\Output\OutputInterface;
use \Magento\Framework\ObjectManagerInterface;

define('STOMP_URI','tcp://admin.bravostore.it:61613');
define('STOMP_USER_NAME','smx');
define('STOMP_PWD','smx');
define('ATTRIBUTE_NAME','Magento_ID');

class Product extends Command
{
     // The Message array
    public $message = array();
        
    
     // @var Magento\CatalogInventory\Api\StockStateInterface     
    protected $_product;
    
    
    // @var Magento\CatalogInventory\Api\StockStateInterface 
    protected $_stockStateInterface;
 
     // @var Magento\CatalogInventory\Api\StockRegistryInterface 
    protected $_stockRegistry;

    protected $_objectManager;
    
    public function __construct(ObjectManagerInterface $manager,\Magento\CatalogInventory\Api\StockRegistryInterface $stockRegistry)
    {
        $this->_objectManager = $manager;
        $this->_stockRegistry = $stockRegistry;
        parent::__construct();
    }
    
    protected function getObjectManager()
    {
        return $this->_objectManager;
    }

    protected function configure()
    {
        $this->setName('product:update')
             ->setDescription('updates a product from queue to Magento');

        parent::configure();
    }



    private function readStompMessage () {
        
        global $message;
        
        $queue  = '/queue/UpdateMage';
        $uri = STOMP_URI;
        $username = STOMP_USER_NAME;
        $passwd = STOMP_PWD;

        try {
            $stompCon = new \Stomp($uri,$username,$passwd);
            }   catch(StompException $e) {
                die('Connection failed: ' . $e->getMessage());
                }
        $stompCon->subscribe($queue);

        $msg = $stompCon->readFrame();
  
        if ( $msg != null) {
         echo "Received array: "; 
         print_r($msg->body);
         // mark the message as received in the queue
         $stompCon->ack($msg);
         } else {
                echo "Failed to receive a message\n";
                }
        // Transform into Array $message        
        $temp = simplexml_load_string($msg->body);
        $json = json_encode($temp);
        $message_temp = json_decode($json,TRUE);
        $message = $message_temp['Product'];

    }


    private function setMagentoProduct() {
   

        global $message;
        
        // Get Storemanager
        $storeManager = $this->_objectManager->get('Magento\Store\Model\StoreManager');
        
        // Get Website ID
        $websiteId = $storeManager->getWebsite()->getWebsiteId();
        
        // Init Product
        $product = $this->_objectManager->create('\Magento\Catalog\Model\Product');   
        //check if product exists
        $productId=$product->getIdBySku($message['SKU']);
       
 
        if($productId) {		  
                  try{     
                    $product->load($productId);
                    } catch(Exception $e) {  
                            echo "Could not load product ".$e."  "; 
                        }                        
            }
        // Set stock data
        try{            
            $stockItem=$this->_stockRegistry->getStockItem($productId,$websiteId); // load stock of that product
            $stockItem->setData('is_in_stock',1); //set updated data as your requirement
            $stockItem->setData('qty',$message['QtyOnHand']); //set updated quantity 
            $stockItem->setData('manage_stock',1);
            $stockItem->setData('use_config_notify_stock_qty',1);
            $stockItem->save(); //save stock of item
            $product->save(); //  also save product
            $code=200;
          
            }catch(Exception $e){
                echo 'Exception during product creation  '.$e.'  ';
                $code=400;
                
                 }
        return($code);         
    }

    protected function execute(InputInterface $input, OutputInterface $output)
    {
        $output->writeln('Hello World!');
        global $message;

        $i_objectManager = $this->getObjectManager();
        $this->readStompMessage();
        //Check if attribute value for Magento is set if yes add product        
        if((bool)$message[ATTRIBUTE_NAME]) {
        $code=$this->setMagentoProduct(); }
    }
}

