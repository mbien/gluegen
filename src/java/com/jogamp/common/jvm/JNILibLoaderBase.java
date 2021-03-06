/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */
package com.jogamp.common.jvm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.util.HashSet;
import java.util.Set;
import jogamp.common.Debug;

public class JNILibLoaderBase {

    public static final boolean DEBUG = Debug.debug("JNILibLoader");

    private static final Set<String> loaded = new HashSet<String>();
    private static LoaderAction loaderAction = new DefaultAction();

    public static boolean isLoaded(String libName) {
        return loaded.contains(libName);
    }

    public static void addLoaded(String libName) {
        loaded.add(libName);
        if (DEBUG) {
            System.err.println("JNILibLoaderBase Loaded Native Library: " + libName);
        }
    }

    public static void disableLoading() {
        setLoadingAction(null);
    }

    public static void enableLoading() {
        setLoadingAction(new DefaultAction());
    }

    public static synchronized void setLoadingAction(LoaderAction action) {
        loaderAction = action;
    }

    protected static synchronized boolean loadLibrary(String libname, boolean ignoreError) {
        if (loaderAction != null) {
            return loaderAction.loadLibrary(libname, ignoreError);
        }
        return false;
    }

    protected static synchronized void loadLibrary(String libname, String[] preload, boolean preloadIgnoreError) {
        if (loaderAction != null) {
            loaderAction.loadLibrary(libname, preload, preloadIgnoreError);
        }
    }


    public interface LoaderAction {

        /**
         * Loads the library specified by libname.
         * The implementation should ignore, if the library has been loaded already.<br>
         * @param libname the library to load
         * @param ignoreError if true, errors during loading the library should be ignored
         * @return true if library loaded successful
         */
        boolean loadLibrary(String libname, boolean ignoreError);

        /**
         * Loads the library specified by libname.
         * Optionally preloads the libraries specified by preload.<br>
         * The implementation should ignore, if any library has been loaded already.<br>
         * @param libname the library to load
         * @param preload the libraries to load before loading the main library if not null
         * @param preloadIgnoreError if true, errors during loading the preload-libraries should be ignored
         */
        void loadLibrary(String libname, String[] preload, boolean preloadIgnoreError);
    }

    private static class DefaultAction implements LoaderAction {

        private static final Method customLoadLibraryMethod;
        private static final AccessControlContext localACC = AccessController.getContext();
        private static final String osAndArch;
        private static final String ext;
        private static final String prefix;

        static {

            Method loadLibraryMethod = null;
            Class<?> launcherClass = null;

            if (Debug.getBooleanProperty("sun.jnlp.applet.launcher", false, localACC)) {
                try {
                    launcherClass = Class.forName("org.jdesktop.applet.util.JNLPAppletLauncher");
                    loadLibraryMethod = launcherClass.getDeclaredMethod("loadLibrary", new Class[]{String.class});
                } catch (ClassNotFoundException ex) {
                    if (DEBUG) {
                        ex.printStackTrace();
                    }
                } catch (NoSuchMethodException ex) {
                    if (DEBUG) {
                        ex.printStackTrace();
                    }
                    launcherClass = null;
                }
            }

            if (null == launcherClass) {
                String launcherClassName = Debug.getProperty("jnlp.launcher.class", false, localACC);
                if (null != launcherClassName) {
                    try {
                        launcherClass = Class.forName(launcherClassName);
                        loadLibraryMethod = launcherClass.getDeclaredMethod("loadLibrary", new Class[]{String.class});
                    } catch (ClassNotFoundException ex) {
                        if (DEBUG) {
                            ex.printStackTrace();
                        }
                    } catch (NoSuchMethodException ex) {
                        if (DEBUG) {
                            ex.printStackTrace();
                        }
                        launcherClass = null;
                    }
                }
            }

            customLoadLibraryMethod = loadLibraryMethod;

            // assamble lib namespace
            String os = System.getProperty("os.name").toLowerCase();
            if(os.contains("windows")) {
                os = "windows";
                ext = "dll";
                prefix = "";
            }else if(os.contains("mac os")) {
                os = "macosx";
                ext = "jnilib";
                prefix = "lib";
            }else {
                os = "linux";
                ext = "so";
                prefix = "lib";
            }

            String arch = System.getProperty("os.arch").toLowerCase();
            if(os.equals("macosx")) {
                arch = "universal";
            } else if(arch.contains("64")) {
                arch = "amd64";
            }else{
                arch = "i586";
            }
            osAndArch = os+"-"+arch;
        }

        public boolean loadLibrary(String libname, boolean ignoreError) {
            boolean res = true;
            if (!isLoaded(libname)) {
                try {
                    loadLibraryImpl(libname);
                    addLoaded(libname);
                    if (DEBUG) {
                        System.err.println(getClass() + " loaded " + libname);
                    }
                } catch (UnsatisfiedLinkError e) {
                    res = false;
                    if (DEBUG) {
                        e.printStackTrace();
                    }
                    if (!ignoreError && e.getMessage().indexOf("already loaded") < 0) {
                        throw e;
                    }
                }
            }
            return res;
        }

        public void loadLibrary(String libname, String[] preload, boolean preloadIgnoreError) {
            if (!isLoaded(libname)) {
                if (null != preload) {
                    for (int i = 0; i < preload.length; i++) {
                        loadLibrary(preload[i], preloadIgnoreError);
                    }
                }
                loadLibrary(libname, false);
            }
        }

        private void loadLibraryImpl(String libraryName) {

            // Note: special-casing JAWT which is built in to the JDK
            if (null != customLoadLibraryMethod && !libraryName.equals("jawt")) {
                loadCustom(libraryName);
            } else{
                try {
                    if(loadFromClasspath(libraryName)) {
                        return;
                    }
                } catch (IOException ex) {
                    // fail hard: lib was found in classpath but could not be extracted
                    throw new RuntimeException("can not load "+libraryName+" from classpath", ex);
                }
                loadFromLibpath(libraryName);
            }
        }

        private void loadCustom(String libraryName) {

            try {
                customLoadLibraryMethod.invoke(null, new Object[]{libraryName});
            } catch (Exception e) {
                Throwable t = e;
                if (t instanceof InvocationTargetException) {
                    t = ((InvocationTargetException) t).getTargetException();
                }
                if (t instanceof Error) {
                    throw (Error) t;
                }
                if (t instanceof RuntimeException) {
                    throw (RuntimeException) t;
                }
                // Throw UnsatisfiedLinkError for best compatibility with System.loadLibrary()
                throw (UnsatisfiedLinkError) new UnsatisfiedLinkError("can not load library " + libraryName).initCause(e);
            }
        }

        private void loadFromLibpath(String libraryName) {
            System.loadLibrary(libraryName);
        }

        /**
         * Attempts classpath libloading, returns true on success.
         * @throws IOException thrown when the lib has been found but something went wrong in the extraction process.
         */
        private boolean loadFromClasspath(String libraryName) throws IOException {

            // cp filesystem
            String path = '/'+osAndArch+'/'+prefix+libraryName+'.'+ext;

            InputStream in = getClass().getResourceAsStream(path);
            if (in != null) {
                File copy = File.createTempFile(prefix+libraryName, '.' + ext);
                copy.deleteOnExit();
                OutputStream out = null;
                try {
                    out = new FileOutputStream(copy);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                    }
                } finally {
                    try{
                        in.close();
                    }finally{
                        if (out != null) {
                            out.close();
                        }
                    }
                }
                System.load(copy.getPath());
                return true;
            }
            return false;
        }
    }
}
