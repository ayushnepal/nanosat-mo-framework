/* ----------------------------------------------------------------------------
 * Copyright (C) 2015      European Space Agency
 *                         European Space Operations Centre
 *                         Darmstadt
 *                         Germany
 * ----------------------------------------------------------------------------
 * System                : ESA NanoSat MO Framework
 * ----------------------------------------------------------------------------
 * Licensed under the European Space Agency Public License, Version 2.0
 * You may not use this file except in compliance with the License.
 *
 * Except as expressly set forth in this License, the Software is provided to
 * You on an "as is" basis and without warranties of any kind, including without
 * limitation merchantability, fitness for a particular purpose, absence of
 * defects or errors, accuracy or non-infringement of intellectual property rights.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * ----------------------------------------------------------------------------
 */
package esa.mo.nmf.nmfpackage;

import esa.mo.nmf.nmfpackage.descriptor.NMFPackageDescriptor;
import esa.mo.nmf.nmfpackage.descriptor.NMFPackageFile;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The NMFPackageManager class allows the install, uninstall and upgrade an NMF
 * Package
 *
 * @author Cesar Coelho
 */
public class NMFPackageManager {

    private static final String INSTALLATION_FOLDER_PROPERTY = "esa.mo.nmf.nmfpackage.installationFolder";
    private static final String INSTALLED_RECEIPTS_FOLDER_PROPERTY = "esa.mo.nmf.nmfpackage.receipts";
    private static final String RECEIPT_ENDING = ".receipt";

    public static void install(final String packageLocation) throws FileNotFoundException, IOException {
        // Get the File to be installed
        ZipFile zipFile = new ZipFile(packageLocation);
        ZipEntry receipt = zipFile.getEntry(HelperNMFPackage.RECEIPT_FILENAME);

        // Verify integrity of the file: Are all the declared files matching their CRCs?
        Logger.getLogger(NMFPackageCreator.class.getName()).log(Level.INFO,
                "Reading the receipt file that includes the list of files to be installed...");

        // Get the text out of that file and parse it into a NMFPackageDescriptor object
        final InputStream stream = zipFile.getInputStream(receipt);
        final NMFPackageDescriptor descriptor = NMFPackageDescriptor.parseInputStream(stream);
        stream.close();

        // Safety check... should never happen...
        if (descriptor == null) {
            throw new IOException("The parsed descriptor is null.");
        }

        // Verify integrity of the file: Are all the declared files matching their CRCs?
        Logger.getLogger(NMFPackageCreator.class.getName()).log(Level.INFO,
                "Verifying the integrity of the files to be installed...");

        // Do the files actually match the descriptor?
        for (int i = 0; i < descriptor.getFiles().size(); i++) {
            NMFPackageFile file = descriptor.getFiles().get(i);
            ZipEntry entry = zipFile.getEntry(file.getPath());

            if (entry == null) {
                throw new IOException("The descriptor is incorrect. One of the files does not exist: " + file.getPath());
            }

            if (file.getCRC() != entry.getCrc()) {
                throw new IOException("The CRC does not match!");
            }
        }

        // Copy the files according to the NMF statement file
        Logger.getLogger(NMFPackageCreator.class.getName()).log(Level.INFO,
                "Copying the files to the new locations...");

        // Default location of the folder
        File folder = new File(".." + File.separator + ".." + File.separator);

        // Read the Property of the folder to install the packages
        if (System.getProperty(INSTALLATION_FOLDER_PROPERTY) != null) {
            folder = new File(System.getProperty(INSTALLATION_FOLDER_PROPERTY));
        }

        File newFile;
        byte[] buffer = new byte[1024];

        // Iterate through the files, unpack them into the right folders
        for (int i = 0; i < descriptor.getFiles().size(); i++) {
            NMFPackageFile file = descriptor.getFiles().get(i);
            ZipEntry entry = zipFile.getEntry(file.getPath());

            File destination = new File(entry.getName());
            newFile = new File(folder.getCanonicalPath() + File.separator + destination.getCanonicalPath());
//            newFile = new File(destination.getAbsolutePath());
            //create the file otherwise we get FileNotFoundException
            new File(newFile.getParent()).mkdirs();

            Logger.getLogger(NMFPackageCreator.class.getName()).log(Level.INFO,
                    "Copying the file to location: " + newFile.getCanonicalPath());

            final FileOutputStream fos = new FileOutputStream(newFile);

            int len;
            final InputStream zis = zipFile.getInputStream(entry);

            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }

            fos.close();

            final long crc = HelperNMFPackage.calculateCRCFromFile(newFile.getCanonicalPath());

            // We will also need to double check the CRCs again against the real files!
            // Just to double-check.. better safe than sorry!
            if (file.getCRC() != crc) {
                throw new IOException("The CRC does not match!");
            }
        }

        // ---------------------------------------
        // Store a copy of the receipt to know that it has been installed!
        // Default location of the folder
        File temp = getReceiptsFolder();
        String receiptFilename = descriptor.getDetails().getPackageName() + NMFPackageManager.RECEIPT_ENDING;
        File receiptFile = new File(temp.getCanonicalPath() + File.separator + receiptFilename);

        //create the file otherwise we get FileNotFoundException
        new File(receiptFile.getParent()).mkdirs();
        // -----------------

        final FileOutputStream fos = new FileOutputStream(receiptFile);
        final InputStream zis = zipFile.getInputStream(receipt);
        int len;

        while ((len = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
        }

        fos.close();
        zis.close();
        // ---------------------------------------

        Logger.getLogger(NMFPackageCreator.class.getName()).log(Level.INFO,
                "Package Successfully installed from location: " + packageLocation);
    }

    public static void uninstall(final String packageLocation, final boolean keepConfigurations) {
        // Get the Package to be uninstalled

        // Delete the files according to the NMF statement file
        // Do we keep the previous configurations?
    }

    public static void upgrade(final String packageLocation) {
        // Get the Files to be installed

        // Upgrade the files according to the NMF statement file
        // Keep the same configurations
    }

    public static boolean isPackageInstalled(String packageLocation) {
        // based on the name, we have to go to the receipts folder and check!

        // Find the receipt out of the package
        ZipFile zipFile;
        try {
            zipFile = new ZipFile(packageLocation);
        } catch (IOException ex) {
            Logger.getLogger(NMFPackageManager.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        ZipEntry receipt = zipFile.getEntry(HelperNMFPackage.RECEIPT_FILENAME);
        NMFPackageDescriptor descriptorFromPackage = null;
        long crcDescriptorFromPackage = 0;

        try {
            InputStream zis = zipFile.getInputStream(receipt);
            descriptorFromPackage = NMFPackageDescriptor.parseInputStream(zis);
            zis.close();
            zis = zipFile.getInputStream(receipt);
            crcDescriptorFromPackage = HelperNMFPackage.calculateCRCFromInputStream(zis);
            zis.close();
        } catch (IOException ex) {
            Logger.getLogger(NMFPackageManager.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        Logger.getLogger(NMFPackageManager.class.getName()).log(Level.INFO,
                "Package name: " + descriptorFromPackage.getDetails().getPackageName());

        File temp = getReceiptsFolder();
        String receiptFilename = descriptorFromPackage.getDetails().getPackageName() + NMFPackageManager.RECEIPT_ENDING;
        File receiptFile;
        try {
            receiptFile = new File(temp.getCanonicalPath() + File.separator + receiptFilename);
        } catch (IOException ex) {
            Logger.getLogger(NMFPackageManager.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        boolean exists = receiptFile.exists();

        if (!exists) {
        Logger.getLogger(NMFPackageManager.class.getName()).log(Level.INFO,
                "The package is not installed!");
            return false;
        }

        Logger.getLogger(NMFPackageManager.class.getName()).log(Level.INFO,
                "The package is installed!");
        
        // We need to double check if the crc match!
//        long crc = HelperNMFPackage.calculateCRCFromInputStream(null);
//        crcDescriptorFromPackage == 
        return true;
    }
    
    private static File getReceiptsFolder(){
        // Default location of the folder
        File folder = new File("receipts");

        // Read the Property of the folder to install the packages
        if (System.getProperty(INSTALLED_RECEIPTS_FOLDER_PROPERTY) != null) {
            folder = new File(System.getProperty(INSTALLED_RECEIPTS_FOLDER_PROPERTY));
        }
        
        return folder;
    }

}
