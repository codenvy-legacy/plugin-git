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

import com.codenvy.ide.ext.ssh.server.SshKey;
import com.codenvy.ide.ext.ssh.server.SshKeyPair;
import com.codenvy.ide.ext.ssh.server.SshKeyStore;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.util.HashSet;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * @author Eugene Voevodin
 */
@Listeners(MockitoTestNGListener.class)
public class SSHTest extends BaseTest {

    @Mock
    SshKeyStore keyStore;

    @Test
    public void testSshKeysManager() throws Exception {
        //given
        SshKey publicKey = new SshKey("public_key", "publicsshkey".getBytes());
        SshKey privateKey = new SshKey("private_key", "privatesshkey".getBytes());

        when(keyStore.genKeyPair(eq("host.com"), eq("comment"), eq("password")))
                .thenReturn(new SshKeyPair(publicKey, privateKey));
        when(keyStore.getPrivateKey(eq("host.com"))).thenReturn(privateKey);
        when(keyStore.getPublicKey(eq("host.com"))).thenReturn(publicKey);
        //generating key
        keyStore.genKeyPair("host.com", "comment", "password");
        //creating SshKeyManager with no uploaders
        SshKeysManager manager = new SshKeysManager(keyStore, new HashSet<SshKeyUploader>());

        //when
        File key = manager.writeKeyFile(DEFAULT_URI);
        //then
        assertEquals(readFile(key).getBytes(),
                keyStore.getPrivateKey("host.com").getBytes());
        forClean.add(key);
    }
}
