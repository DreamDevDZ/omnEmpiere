<?php

require_once 'app/Mage.php';

class OmnEmpiereAddProduct {
    
    
    public $message = array();
    
    
    public function __construct() {
        set_time_limit(0);       
    }
    
    

    private function downloadFile ($url, $path) {

    $newfname = $path;
   
        try {    
        $file = fopen ($url, "rb");
         
            
        if ($file) {
        $newf = fopen ($newfname, "wb");
        

        if ($newf)
        while(!feof($file)) {
        fwrite($newf, fread($file, 1024 * 32 ), 1024 * 32 );
        }
    }

    if ($file) {
        fclose($file);
    }

    if ($newf) {
        fclose($newf);
        }

    echo("Fielcopy done");

    }catch(Exception $e){
            print_r($e->getMessage());                  
            }
    }
    
    private function readStompMessage () {
        
        global $message;
        
        $queue  = '/queue/ProductMage';
        $uri = 'tcp://Your ServiceMix URI:61613';  // add your servicemix URI here
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
            // mark the message as received in the queue
            $stompCon->ack($msg);
        } else {
            echo "Failed to receive a message\n";
            }
        $temp = simplexml_load_string($msg->body);
        $json = json_encode($temp);
        $message = json_decode($json,TRUE);
    }

    
    private function setMagentoProduct() {
   

        global $message;


//Debug        
//        print_r($message);
//        
//        echo "   SKU: ";
//        print_r($message[M_Product_ID]);
//        echo "   Product Name: ";
//        print_r($message[Name]);
        
//prepare Environment

        Mage::register('isSecureArea', true);       
        Mage::app()->setCurrentStore(Mage_Core_Model_App::ADMIN_STORE_ID);
        
        $product = Mage::getModel('catalog/product');
        $setid = Mage::getModel('catalog/product')->getDefaultAttributeSetId();

//prepare and copy Image        
       $imagefile=Mage::getBaseDir('media') . DS . 'import'. DS . basename($message[ImageURL]);  
       $this->downloadFile($message[ImageURL],'FILE://' . $imagefile);	       
//       print_r($imagefile);

//Set Category Id Array
       $parentCategoryId = Mage::app()->getStore("default")->getRootCategoryId();

       $category = Mage::getResourceModel('catalog/category_collection')->addFieldToFilter('name', $message[CategoryName]);
       $cat= $category->getData();
       $categoryId = $cat[0][entity_id];
       $categoryIds = array($parentCategoryId,$categoryId);
//       echo "Categories: ";
//       print_r($categoryIds);
//        $categoryIds = array('2','4');

//check if product exists
        $productId=$product->getIdBySku($message[M_Product_ID]);
//      echo "ProductID: ";
//      print_r($productId);
        if($productId) {
		  echo "Product exists";
                  try{     
                    $product->load($productId)->delete(); 
                    $product = Mage::getModel('catalog/product');    
                     }catch(Exception $e){     echo "Delete failed"; }                        
            }
// create product    
        try{
                $product
                ->setStoreId(1)  // Default Store
                ->setWebsiteIds(array(1)) //website ID the product is assigned to, as an array
                ->setAttributeSetId($setid) //ID of a attribute set named 'default'
                ->setTypeId('simple') //product type
                ->setCreatedAt(strtotime('now')) //product creation time
                ->setUpdatedAt(strtotime('now')) //product update time
                ->setSku($message[M_Product_ID]) //SKU
                ->setName($message[Name]) //product name
                ->setWeight($message[Weight])
                ->setStatus(1) //product status (1 - enabled, 2 - disabled)
//                ->setTaxClassId(($message[TaxName]=='Standard'*2)) tax class (0 - none, 1 - default, 2 - taxable, 4 - shipping)
                ->setTaxClassId('2')  // Taxable
                ->setVisibility(Mage_Catalog_Model_Product_Visibility::VISIBILITY_BOTH) 
                ->setNewsFromDate(strtotime('now')) //product set as new from
                ->setNewsToDate(strtotime('+30 days')) //product set as new to
                ->setPrice($message[PriceStd]) 
//                  ->setCost(22.33) //price in form 11.22
//                  ->setSpecialPrice(00.44) //special price in form 11.22
//                  ->setSpecialFromDate('06/1/2014') //special price from (MM-DD-YYYY)
//                  ->setSpecialToDate('06/30/2014') //special price to (MM-DD-YYYY)
                ->setMsrpEnabled(1) //enable MAP
                ->setMsrpDisplayActualPriceType(1) //display actual price (1 - on gesture, 2 - in cart, 3 - before order confirmation, 4 - use config)
//                  ->setMsrp(99.99) //Manufacturer's Suggested Retail Price
                ->setMetaTitle($message[Description])
                ->setMetaKeyword($message[Description])
                ->setMetaDescription($message[Description])
                ->setDescription($message[Description])
                ->setShortDescription($message[Description])
                ->setMediaGallery (array('images'=>array (), 'values'=>array ()))
                ->addImageToMediaGallery($imagefile, array('small_image','thumbnail','image'), true, false)    
                ->setStockData(array(
                       'use_config_manage_stock' => 0, //'Use config settings' checkbox
                       'manage_stock'=>1, //manage stock
                       'min_sale_qty'=>1, //Minimum Qty Allowed in Shopping Cart
                       'max_sale_qty'=>2, //Maximum Qty Allowed in Shopping Cart
                       'is_in_stock' => 1, //Stock Availability
                       'qty' => $message[QtyOnHand] //qty
                            )
                    )
                 ->setCategoryIds($categoryIds)
                                            ;                   
            $product->save();    
            $product->setMediaGallery (array('images'=>array (), 'values'=>array ()));
            $product->addImageToMediaGallery($imagefile, array('small_image','thumbnail','image'), true, false);
            $product->save();


            }catch(Exception $e){
                print_r($e->getMessage());                  
                //Mage::log($e->getMessage());
                 }
        Mage::unregister('isSecureArea'); 
    }
    
    private function reindexMagento() {

      Mage::app()->setCurrentStore(Mage_Core_Model_App::ADMIN_STORE_ID);
      $indexCollection = Mage::getModel('index/process')->getCollection();
     
      foreach ($indexCollection as $index) {
                  $index->reindexAll();
echo "reindex: ";
                     }
      Mage::app()->cleanCache();
	}
    
    public function run() {
        
        $this->readStompMessage();
        $this->setMagentoProduct();
    }
    
}

$result = new OmnEmpiereAddProduct();
$result->run();
?>
