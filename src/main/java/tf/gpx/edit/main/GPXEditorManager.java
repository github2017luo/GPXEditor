/*
 * Copyright (c) 2014ff Thomas Feuster
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package tf.gpx.edit.main;

import com.gluonhq.charm.down.ServiceFactory;
import com.gluonhq.charm.down.Services;
import com.gluonhq.charm.down.plugins.StorageService;
import com.sun.javafx.PlatformUtil;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import tf.gpx.edit.helper.GPXEditorParameters;
import tf.gpx.edit.helper.GPXEditorPreferences;

/**
 *
 * @author Thomas
 */
public class GPXEditorManager extends Application {
    private final static GPXEditorParameters myParameters = GPXEditorParameters.getInstance();
    private final static GPXEditorPreferences myPreferences = GPXEditorPreferences.getInstance();
    
    private GPXEditor controller;
    private Stage myStage;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // get rid of INFO messages from gluon maps
        // https://stackoverflow.com/questions/13760095/java-dynamically-change-logging-level
        final Handler[] handlers = Logger.getLogger("").getHandlers();
        for (Handler handler : handlers) {
            handler.setLevel(Level.WARNING);
        }
        
        // set cache for mapo tile to avoid error message java.io.IOException: Storage Service is not available
        // https://github.com/gluonhq/maps/issues/8#issuecomment-310389905
        if(PlatformUtil.isWindows() || PlatformUtil.isMac() || PlatformUtil.isUnix()) {
            System.setProperty("javafx.platform" , "Desktop");
        }

        // define service for desktop
        StorageService storageService = new StorageService() {
            @Override
            public Optional<File> getPrivateStorage() {
                // user home app config location (linux: /home/[yourname]/.gluonmaps)
                return Optional.of(new File(System.getProperty("user.home")));
            }

            @Override
            public Optional<File> getPublicStorage(String subdirectory) {
                // this should work on desktop systems because home path is public
                return getPrivateStorage();
            }

            @Override
            public boolean isExternalStorageWritable() {
                //noinspection ConstantConditions
                return getPrivateStorage().get().canWrite();
            }

            @Override
            public boolean isExternalStorageReadable() {
                //noinspection ConstantConditions
                return getPrivateStorage().get().canRead();
            }
        };

        // define service factory for desktop
        ServiceFactory<StorageService> storageServiceFactory = new ServiceFactory<StorageService>() {
            @Override
            public Class<StorageService> getServiceType() {
                return StorageService.class;
            }

            @Override
            public Optional<StorageService> getInstance() {
                return Optional.of(storageService);
            }
        };

        // register service
        Services.registerServiceFactory(storageServiceFactory);
        
        launch(GPXEditorManager.class, args);
    }
    
    /**
     * 
     * @param primaryStage 
     */
    @Override
    public void start(Stage primaryStage) {
        // now we have three kinds of parameters :-(
        // 1) named: name, value pairs from jnlp
        // 2) unnamed: values only from jnlp
        // 3) raw: good, old command line parameters
        // http://java-buddy.blogspot.de/2014/02/get-parametersarguments-in-javafx.html

        // for now just use raw parameters since the code as already there for this :-)
        // let some one else deal with the command line parameters
        Parameters myParams = getParameters();
        if ((myParams != null) && (myParams.getRaw() != null) && !myParams.getRaw().isEmpty()) {
            myParameters.init(myParams.getRaw().toArray(new String[0]));
        } else {
            myParameters.init(null);
        }
        
        if (myParameters.doBatch()) {
            // batch call! do things and then go home...
            GPXEditorBatch.getInstance().executeBatchProecssing();
            
            stop();
            Platform.exit();
        } else {
            FXMLLoader fxmlLoader = null;
            BorderPane pane = null;
            try {
                fxmlLoader = new FXMLLoader(GPXEditorManager.class.getResource("/GPXEditor.fxml"));
                pane =(BorderPane) fxmlLoader.load();

                // set passed parameters for later use
                controller = fxmlLoader.getController();
            } catch (IOException ex) {
                Logger.getLogger(GPXEditorManager.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(-1); 
            }

            // TF, 20161103: store and read height, width of scene and divider positions of splitpane
            Double recentWindowWidth = Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.RECENTWINDOWWIDTH, "1200"));
            Double recentWindowHeigth = Double.valueOf(GPXEditorPreferences.get(GPXEditorPreferences.RECENTWINDOWHEIGTH, "600"));

            primaryStage.setScene(new Scene(pane, recentWindowWidth, recentWindowHeigth));
            primaryStage.setTitle("GPX Editor"); 
            primaryStage.getIcons().add(new Image(GPXEditorManager.class.getResourceAsStream("/GPXEditorManager.png")));
            primaryStage.getScene().getStylesheets().add(GPXEditorManager.class.getResource("/GPXEditor.css").toExternalForm());
            primaryStage.show();

            // store for later reference
            myStage = primaryStage;
        }
    }
    
    @Override
    public void stop() {
        // TF, 20161103: store and read height, width of scene and divider positions of splitpane
        if (myStage != null) {
            GPXEditorPreferences.put(GPXEditorPreferences.RECENTWINDOWWIDTH, Double.toString(myStage.getScene().getWidth()));
            GPXEditorPreferences.put(GPXEditorPreferences.RECENTWINDOWHEIGTH, Double.toString(myStage.getScene().getHeight()));
        }
        
        if (controller != null) {
            // TF, 20161103: call controller to store window values
            controller.stop();
        }
    }
}