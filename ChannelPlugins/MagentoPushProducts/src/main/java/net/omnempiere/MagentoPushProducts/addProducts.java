/******************************************************************************
 * Product: OmnEmpiere - sub-project of ADempiere                              *
 * Copyright (C) ALL GPL FOSS PROJECTS where taken                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

package net.omnempiere.MagentoPushProducts;

import java.util.logging.Logger;
import java.io.*;

import org.apache.commons.io.IOUtils;

public class addProducts {

    
    private static final transient Logger logger = Logger.getLogger(addProducts.class.getName());
    private boolean verbose = true;
    private String prefix = "AddProducts";
    private static String addProdcutURI="https://Your Magento URI/OmnEmpiereAddProductWeb.php";  // Add your Magento URI here
    private static String callReindexURI="https://Your Magento URI/OmnEmpiereCallReIndex.php";   // Add your Magento URI here
   

    public void callAddProduct(String body) {
	logger.info("Working Directory = " + System.getProperty("user.dir"));
	try {       
		/**		Process p = new ProcessBuilder("/etc/omnempiere.d/pushproduct.sh").start();  **/
		Process p = new ProcessBuilder("/usr/bin/curl","--fail", "--silent", "--show-error",addProdcutURI).start();
		p.waitFor();
		BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		BufferedReader output =  new BufferedReader(new InputStreamReader(p.getInputStream()));
		
	    logger.severe(IOUtils.toString(error));
	    logger.info(IOUtils.toString(output));
	    } catch (Exception e) {
    logger.severe("call failed: " + e.getMessage()); }       
	} 
    
    public void callMagentoReIndex(String body) {
	logger.info("This is reindexing");
        try {
        	/** Process p = new ProcessBuilder("/etc/omnempiere.d/reindex.sh").start(); **/
        	Process p = new ProcessBuilder("/usr/bin/curl","--fail", "--silent", "--show-error",callReindexURI).start();
        	BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        	BufferedReader output =  new BufferedReader(new InputStreamReader(p.getInputStream()));
		
        	logger.severe(IOUtils.toString(error));
        	logger.info(IOUtils.toString(output));
        	} catch (IOException e) {
    logger.severe("call failed: " + e.getMessage()); }
      }

  public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    }

   
