<?php
namespace OmnEmpiere\Tools2\Model;

use \Symfony\Component\Console\Command\Command;
use \Symfony\Component\Console\Input\InputInterface;
use \Symfony\Component\Console\Output\OutputInterface;
use \Magento\Framework\ObjectManagerInterface;

define('DS', DIRECTORY_SEPARATOR);

define('STOMP_URI_2','tcp://admin.bravostore.it:61613');
define('STOMP_USER_NAME_2','smx');
define('STOMP_PWD_2','smx');
define('ATTRIBUTE_NAME_2','Magento_ID');

class Newitem extends Command
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
        $this->setName('newitem:add')
             ->setDescription('adds a product from queue to Magento');

        parent::configure();
    }


    private function downloadFile ($url, $path) {

    $newfname = $path;
   
    try {    
        $file = fopen ($url, "rb");
         
            
        if ($file) {
        $newf = fopen ($newfname, "wb");
        

            if ($newf)  {               
                while(!feof($file)) {
                fwrite($newf, fread($file, 1024 * 32 ), 1024 * 32 );
                    }
                }   
            }   

        if ($file) {
            fclose($file);
            }

        if ($newf) {
            fclose($newf);
            }

        echo("Fielcopy done");

        }   catch(Exception $e) {
            echo 'Exception during file copy:  '.$e;                  
            }
    }

    private function readStompMessage () {
        
        global $message;
        
        $queue  = '/queue/ProductMage';
        $uri = STOMP_URI_2;
        $username = STOMP_USER_NAME_2;
        $passwd = STOMP_PWD_2;

        try {
            $stompCon = new \Stomp($uri,$username,$passwd);
            }   catch(\StompException $e) {
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
        $message = json_decode($json,TRUE);
    }

private function setMagentoProduct() {
   

        global $message;

        //$objectManager = \Magento\Framework\App\ObjectManager::getInstance(); // instance of object manager
        
        $storeManager = $this->_objectManager->get('Magento\Store\Model\StoreManager');
        
        /// Get Website ID
        $websiteId = $storeManager->getWebsite()->getWebsiteId();
        //echo 'websiteId: '.$websiteId."   ";

        /// Get Store ID
        $store = $storeManager->getStore();
        $storeId = $store->getStoreId();
            //echo 'storeId: '.$storeId."   ";

        /// Get Root Category ID
        $rootNodeId = $store->getRootCategoryId();
        //echo 'rootNodeId: '.$rootNodeId."   ";

        /// Get Root Category Name (not used, just ion case required)
    
        //$rootCat = $objectManager->get('Magento\Catalog\Model\Category');
        //$cat_info = $rootCat->load($rootNodeId);
        
        //Find list of Categories with Name [ATTRIBUTE_NAME_2]
       echo 'The Catgeory Name:  '.$message[ATTRIBUTE_NAME_2].'  ';
       $category = $this->_objectManager->create('Magento\Catalog\Model\Category');
       $categ=$category->getCollection()->addAttributeToFilter('name',$message[ATTRIBUTE_NAME_2])->getFirstItem();     
       $categoryId = $categ->getId();
        
        
       if(isset($categoryId)) {
       // Category exists
            $categoryIds = array($categoryId,$rootNodeId);
            echo ' The category exists, ID: '.$categoryId.'  ';
         }
         else {
             /// Add a new sub category under root category
            echo 'Create new Category   ';
            $catData = [
            'data' => [
                "parent_id" => $rootNodeId,
                'name' => $message[ATTRIBUTE_NAME_2],
                "is_active" => true,
                "include_in_menu" => true,
            ],
            'custom_attributes' => [
//                "display_mode"=> "PRODUCTS",
//                "is_anchor"=> "1",
//                "custom_use_parent_settings"=> "0",
//                "custom_apply_to_products"=> "0",
//                "url_key"=> "", // if not set magento uses the name to generate it
//                "url_path"=> "cat2",
//                "automatic_sorting"=> "0",
//                'my_own_attribute' => 'value' // <-- your attribute
                ]
            ];
            $category_new = $this->_objectManager->create('Magento\Catalog\Model\Category',$catData);

            $repository = $this->_objectManager->get('\Magento\Catalog\Model\CategoryRepository');
            $result = $repository->save($category_new);

            // and get the categoryID of the created category
            $categoryId = $category_new->getId();
            $categoryIds = array($categoryId);
            echo 'and the new category id is:  '.$categoryId.'  ';
         }
        
        
        
        //prepare and copy product image
    
        $imagedirectory=$this->_objectManager->get('Magento\Framework\Filesystem')->getDirectoryRead(\Magento\Framework\App\Filesystem\DirectoryList::MEDIA)->getAbsolutePath();    
        $imagefile=  $imagedirectory . DS . 'import'. DS . basename($message['ImageURL']);         
        $this->downloadFile($message['ImageURL'],'FILE://' . $imagefile);	       

       
       ///Init Product
       
        $product = $this->_objectManager->create('\Magento\Catalog\Model\Product');   
        $setid = $product->getResource()->getEntityType()->getDefaultAttributeSetId();
        //Set magento taxclassid according to TaxName, default 2
        $taxid=2;
        if($message['TaxName']=='Exempted') {
                $taxid=0;                
                } 
        if($message['TaxName']=='Standard') {
                $taxid=2;                
                }
         if($message['TaxName']=='Reduced1') {
                $taxid=3;                
                }  
         if($message['TaxName']=='Reduced2') {
                $taxid=4;                
                } 
           if($message['TaxName']=='Reduced3') {
                $taxid=5;                
                }      

        //check if product exists
        $productId=$product->getIdBySku($message['M_Product_ID']);
        //      echo "ProductID: ".$productId.'   ';
        if($productId) {
		  echo "Product exists";
                  try{     
                    $product->load($productId)->delete(); // delete product 
                    $product = $this->_objectManager->create('\Magento\Catalog\Model\Product'); // and create a new one    
                     }catch(Exception $e){     echo "Delete failed ".$e.'  '; }                        
            }
        // create product    
        try{
                $product
                ->setStoreId($storeId)  // Default Store
                ->setWebsiteIds(array($websiteId)) //website ID the product is assigned to, as an array
                ->setAttributeSetId($setid) //ID of a attribute set named 'default'
                ->setTypeId('simple') //product type
                ->setCreatedAt(strtotime('now')) //product creation time
                ->setUpdatedAt(strtotime('now')) //product update time
                ->setSku($message['M_Product_ID']) //SKU
                ->setName($message['Name']) //product name
                ->setWeight($message['Weight'])
                ->setStatus(1) //product status (1 - enabled, 2 - disabled)
                ->setTaxClassId($taxid)  // Taxable 
                ->setNewsFromDate(strtotime('now')) //product set as new from
                ->setNewsToDate(strtotime('+30 days')) //product set as new to
                ->setPrice($message['PriceStd']) 
                //->setCost(22.33) //price in form 11.22
                //->setSpecialPrice(00.44) //special price in form 11.22
                //->setSpecialFromDate('06/1/2014') //special price from (MM-DD-YYYY)
                //->setSpecialToDate('06/30/2014') //special price to (MM-DD-YYYY)
                ->setMsrpEnabled(1) //enable MAP
                ->setMsrpDisplayActualPriceType(1) //display actual price (1 - on gesture, 2 - in cart, 3 - before order confirmation, 4 - use config)
                //->setMsrp(99.99) //Manufacturer's Suggested Retail Price
                ->setMetaTitle($message['Description'])
                ->setMetaKeyword($message['Description'])
                ->setMetaDescription($message['Description'])
                ->setDescription($message['Description'])
                ->setShortDescription($message['Description'])
                ->setMediaGallery (array('images'=>array (), 'values'=>array ()))
                ->addImageToMediaGallery($imagefile, array('small_image','thumbnail','image'), true, false)    
                ->setStockData(array(
                       'use_config_manage_stock' => 0, //'Use config settings' checkbox
                       'manage_stock'=>1, //manage stock
                       'min_sale_qty'=>1, //Minimum Qty Allowed in Shopping Cart
                       'max_sale_qty'=>$message['QtyOnHand'], //Maximum Qty Allowed in Shopping Cart
                       'is_in_stock' => 1, //Stock Availability
                       'qty' => $message['QtyOnHand'] //qty
                            )
                    )
                 ->setCategoryIds($categoryIds)
                                            ;                   
            $product->save();    
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
        if((bool)$message[ATTRIBUTE_NAME_2]) {
        $code=$this->setMagentoProduct(); }
    }
}

