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

import com.codenvy.ide.ext.git.server.GitException;
import com.codenvy.ide.ext.git.shared.RmRequest;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Eugene Voevodin
 */
public class RemoveTest extends BaseTest {

    @Test
    public void testNotCachedRemove() throws GitException {
        getConnection().rm(newDTO(RmRequest.class).withItems(Arrays.asList("README.txt")).withCached(false));
        assertFalse(new File(getRepository().toFile(), "README.txt").exists());
        checkNotCached(getRepository().toFile(), "README.txt");
    }

    @Test
    public void testCachedRemove() throws GitException {
        getConnection().rm(newDTO(RmRequest.class).withItems(Arrays.asList("README.txt")).withCached(true));
        assertTrue(new File(getRepository().toFile(), "README.txt").exists());
        checkNotCached(getRepository().toFile(), "README.txt");
    }
}
