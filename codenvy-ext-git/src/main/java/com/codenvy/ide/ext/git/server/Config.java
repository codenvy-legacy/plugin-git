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
package com.codenvy.ide.ext.git.server;

import java.io.File;
import java.util.List;

/**
 * @author andrew00x
 */
public abstract class Config {
    protected final File repository;

    /**
     * @param repository
     *         git repository
     */
    protected Config(File repository) throws GitException {
        this.repository = repository;
    }

    /** */
    public abstract String get(String name) throws GitException;

    public abstract List<String> getAll(String name) throws GitException;

    /**
     * @param name
     *         git config file parameter such as user.name
     * @param value
     *         value that will be written into git config file
     * @throws GitException
     *         when some error occurs
     */
    public abstract Config set(String name, String value) throws GitException;

    public abstract Config add(String name, String value) throws GitException;

    public abstract Config unset(String name) throws GitException;
}
