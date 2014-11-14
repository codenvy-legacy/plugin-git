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
package com.codenvy.ide.ext.github.client.patcher;

import com.google.gwt.user.client.Window;
import com.googlecode.gwt.test.patchers.PatchClass;
import com.googlecode.gwt.test.patchers.PatchMethod;

/**
 * Patcher for Window.Location class.
 *
 * @author Roman Nikitenko
 */
@PatchClass(Window.Location.class)
public class WindowPatcher {

    /** Patch getProtocol method. */
    @PatchMethod(override = true)
    public static String getProtocol() {
        return "";
    }

    /** Patch getHost method. */
    @PatchMethod(override = true)
    public static String getHost() {
        return "";
    }
}
