/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.ext.git.server.nativegit;

import com.codenvy.api.core.UnauthorizedException;
import com.codenvy.commons.env.EnvironmentContext;
import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.ssh.server.SshKey;
import com.codenvy.ide.ext.ssh.server.SshKeyPair;
import com.codenvy.ide.ext.ssh.server.SshKeyStore;
import com.codenvy.ide.ext.ssh.server.SshKeyStoreException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Loads ssh keys into filesystem.
 *
 * @author Eugene Voevodin
 */
@Singleton
public class SshKeysManager {

    private static final Logger  LOG     = LoggerFactory.getLogger(SshKeysManager.class);
    private static final Pattern SSH_URL = Pattern.compile("((((git|ssh)://)(([^\\\\/@:]+@)??)[^\\\\/@:]+)(:|/)|" +
                                                           "([^\\\\/@:]+@[^\\\\/@:]+):)[^\\\\@:]+");

    private static final String DEFAULT_KEY_DIRECTORY_PATH = System.getProperty("java.io.tmpdir");
    private static final String DEFAULT_KEY_NAME           = "identity";

    private static String keyDirectoryPath; // TODO(GUICE): initialize

    private final SshKeyStore         sshKeyStore;
    private final Set<SshKeyUploader> sshKeyUploaders;

    @Inject
    public SshKeysManager(SshKeyStore sshKeyStore, Set<SshKeyUploader> sshKeyUploaders) {
        this.sshKeyStore = sshKeyStore;
        this.sshKeyUploaders = sshKeyUploaders;
    }

    public static String getKeyDirectoryPath() throws GitException {
        return (keyDirectoryPath == null ? DEFAULT_KEY_DIRECTORY_PATH : keyDirectoryPath) + '/'
               + EnvironmentContext.getCurrent().getUser().getName();
    }

    /**
     * Stores ssh key into filesystem.
     *
     * @param uri
     *         link to resource
     * @return path to ssh key
     */
    public String storeKeyIfNeed(String uri) throws UnauthorizedException, GitException {
        final String host = getHost(uri);
        if (host == null) {
            return null;
        }

        SshKeyUploader uploader = null;

        for (Iterator<SshKeyUploader> itr = sshKeyUploaders.iterator(); uploader == null && itr.hasNext(); ) {
            SshKeyUploader next = itr.next();
            if (next.match(uri)) {
                uploader = next;
            }
        }

        if (uploader == null) {
            LOG.warn(String.format("Not found ssh key uploader for %s", host));
            // Git action might fail without SSH key.
            return null;
        }

        SshKey publicKey;
        SshKey privateKey;

        // check keys existence and generate if need
        try {
            if ((privateKey = sshKeyStore.getPrivateKey(host)) != null) {
                publicKey = sshKeyStore.getPublicKey(host);
                if (publicKey == null) {
                    sshKeyStore.removeKeys(host);
                    SshKeyPair sshKeyPair = sshKeyStore.genKeyPair(host, null, null);
                    publicKey = sshKeyPair.getPublicKey();
                    privateKey = sshKeyPair.getPrivateKey();
                }
            } else {
                SshKeyPair sshKeyPair = sshKeyStore.genKeyPair(host, null, null);
                publicKey = sshKeyPair.getPublicKey();
                privateKey = sshKeyPair.getPrivateKey();
            }
        } catch (SshKeyStoreException e) {
            throw new GitException(e.getMessage(), e);
        }

        // upload public key
        try {
            uploader.uploadKey(publicKey);
        } catch (IOException e) {
            throw new GitException(e.getMessage(), e);
        }

        // create directories if need
        final File keyDirectory = new File(getKeyDirectoryPath(), host);
        if (!keyDirectory.exists()) {
            keyDirectory.mkdirs();
        }

        // save private key in local file
        final File keyFile = new File(getKeyDirectoryPath() + '/' + host + '/' + DEFAULT_KEY_NAME);
        try (FileOutputStream fos = new FileOutputStream(keyFile)) {
            fos.write(privateKey.getBytes());
        } catch (IOException e) {
            LOG.error("Cant store key", e);
            throw new GitException("Cant store ssh key. ");
        }

        //set perm to -r--r--r--
        keyFile.setReadOnly();
        //set perm to ----------
        keyFile.setReadable(false, false);
        //set perm to -r--------
        keyFile.setReadable(true, true);
        //set perm to -rw-------
        keyFile.setWritable(true, true);
        return keyFile.toString();
    }

    /**
     * Parses URL and get host from it, if it is possible
     *
     * @param url
     *         URL
     * @return host if it exists in URL or <code>null</code> if it doesn't.
     */
    private String getHost(String url) {
        if (SSH_URL.matcher(url).matches()) {
            int start;
            if ((start = url.indexOf("://")) != -1) {
                /*
                    Host between ("://" or "@") and (":" or "/")
                    for ssh or git Schema uri.
                    ssh://user@host.com/some/path
                    ssh://host.com/some/path
                    git://host.com/user/repo
                    can be with port
                    ssh://host.com:port/some/path
                 */
                int endPoint = url.lastIndexOf(":") != start ? url.lastIndexOf(":") : url.indexOf("/", start + 3);
                int startPoint = !url.contains("@") ? start + 3 : url.indexOf("@") + 1;
                return url.substring(startPoint, endPoint);
            } else {
                /*
                    Host between "@" and ":"
                    user@host.com:login/repo
                 */
                return url.substring(url.indexOf("@") + 1, url.indexOf(":"));
            }
        }
        return null;
    }
}
