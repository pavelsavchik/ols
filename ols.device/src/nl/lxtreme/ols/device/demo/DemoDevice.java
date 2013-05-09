/*
 * OpenBench LogicSniffer / SUMP project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 * 
 * Copyright (C) 2010-2011 - J.W. Janssen, http://www.lxtreme.nl
 */
package nl.lxtreme.ols.device.demo;


import java.io.*;

import nl.lxtreme.ols.common.*;
import nl.lxtreme.ols.common.acquisition.*;
import nl.lxtreme.ols.device.api.*;


/**
 * A device for demo purposes, providing all kinds of static/generated
 * acquisition data.
 */
public class DemoDevice implements Device
{
  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public AcquisitionData acquireData( final Configuration aConfiguration, final DeviceProgressListener aProgressListener )
      throws IOException, InterruptedException
  {
    return new DemoAcquisitionTask( aConfiguration, aProgressListener ).call();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void cancelAcquisition() throws IllegalStateException
  {
    // Nop
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName()
  {
    return "Demo Device";
  }
}

/* EOF */