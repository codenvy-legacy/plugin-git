/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package com.codenvy.ide.ext.git.shared;

import com.codenvy.dto.shared.DTO;

import java.util.List;

/**
 *
 */
@DTO
public interface ConfigRequest {

    boolean isGetAll();

    void setGetAll(boolean geAll);

    ConfigRequest withGetAll(boolean geAll);

    List<String> getConfigEntry();

    void setConfigEntry(List<String> configEntry);

    ConfigRequest withConfigEntry(List<String> configEntry);

}
