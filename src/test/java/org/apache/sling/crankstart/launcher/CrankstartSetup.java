package org.apache.sling.crankstart.launcher;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Setup a Crankstart-launched instance for our tests */ 
public class CrankstartSetup {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final int port = getAvailablePort();
    private final String storagePath = getOsgiStoragePath(); 
    private Thread crankstartThread;
    private final String baseUrl = "http://localhost:" + port;
    
    private static List<CrankstartSetup> toCleanup = new ArrayList<CrankstartSetup>();
    
    public static final String [] MODEL_PATHS = {
        "/crankstart-model.txt",
        "/provisioning-model/base.txt",
        "/provisioning-model/sling-extensions.txt",
        "/provisioning-model/start-level-99.txt",
        "/provisioning-model/crankstart-tests.txt"
    };
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + ", port " + port + ", OSGi storage " + storagePath;
    }
            
    private static int getAvailablePort() {
        int result = -1;
        ServerSocket s = null;
        try {
            try {
                s = new ServerSocket(0);
                result = s.getLocalPort();
            } finally {
                if(s != null) {
                    s.close();
                }
            }
        } catch(Exception e) {
            throw new RuntimeException("getAvailablePort failed", e);
        }
        return result;
    }
    
    private static void mergeModelResource(Launcher launcher, String path) throws Exception {
        final InputStream is = CrankstartSetup.class.getResourceAsStream(path);
        assertNotNull("Expecting test resource to be found:" + path, is);
        final Reader input = new InputStreamReader(is);
        try {
            Launcher.mergeModel(launcher.getModel(), input, path);
            launcher.computeEffectiveModel();
        } finally {
            input.close();
        }
    }
    
    String getBaseUrl() {
        return baseUrl;
    }
     
    synchronized void setup() throws Exception {
        if(crankstartThread != null) {
            return;
        }
        
        synchronized (toCleanup) {
            if(!toCleanup.isEmpty()) {
                log.info("Stopping other Crankstart instances before starting this one...");
            }
            for(CrankstartSetup s : toCleanup) {
                s.stopCrankstartInstance();
            }
            toCleanup.clear();
        }
        
        log.info("Starting {}", this);
        
        final HttpUriRequest get = new HttpGet(baseUrl);
        System.setProperty("crankstart.model.http.port", String.valueOf(port));
        System.setProperty("crankstart.model.osgi.storage.path", storagePath);
        
        try {
            new DefaultHttpClient().execute(get);
            fail("Expecting connection to " + port + " to fail before starting HTTP service");
        } catch(IOException expected) {
        }
        
        final Launcher launcher = new Launcher();
        for(String path : MODEL_PATHS) {
            mergeModelResource(launcher, path);
        }
        launcher.computeEffectiveModel();
        
        crankstartThread = new Thread() {
            public void run() {
                try {
                    launcher.launch();
                } catch(InterruptedException e) {
                    log.info("Launcher thread was interrupted, exiting");
                } catch(Exception e) {
                    e.printStackTrace();
                    fail("Launcher exception:" + e);
                }
            }
        };
        crankstartThread.setDaemon(true);
        crankstartThread.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                stopCrankstartInstance();
            }
        });
    }
    
    private void stopCrankstartInstance() {
        log.info("Stopping {}", this);
        if(crankstartThread == null) {
            return;
        }
        crankstartThread.interrupt();
        try {
            crankstartThread.join();
        } catch(InterruptedException ignore) {
        }
        crankstartThread = null;
    }
    
    private static String getOsgiStoragePath() {
        final File tmpRoot = new File(System.getProperty("java.io.tmpdir"));
        final Random random = new Random();
        final File tmpFolder = new File(tmpRoot, System.currentTimeMillis() + "_" + random.nextInt());
        if(!tmpFolder.mkdir()) {
            fail("Failed to create " + tmpFolder.getAbsolutePath());
        }
        tmpFolder.deleteOnExit();
        return tmpFolder.getAbsolutePath();
    }
}