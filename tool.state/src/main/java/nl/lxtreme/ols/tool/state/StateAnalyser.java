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
package nl.lxtreme.ols.tool.state;


import java.awt.*;

import nl.lxtreme.ols.api.acquisition.*;
import nl.lxtreme.ols.api.data.*;
import nl.lxtreme.ols.api.tools.*;
import nl.lxtreme.ols.tool.base.*;


/**
 * Provides a state analysis tool (??? not sure what it does though ???).
 */
public class StateAnalyser extends BaseAsyncTool<StateAnalysisDialog, AcquisitionResult, StateAnalysisWorker>
{
  // CONSTRUCTORS

  /**
   * Creates a new StateAnalyser instance.
   */
  public StateAnalyser()
  {
    super( ToolCategory.OTHER, "State analyser ..." );
  }

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  protected StateAnalysisDialog createDialog( final Window aOwner, final ToolContext aContext, final String aName )
  {
    return new StateAnalysisDialog( aOwner, getName(), aContext );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected StateAnalysisWorker createToolWorker( final DataContainer aData, final ToolContext aContext )
  {
    return new StateAnalysisWorker( aData, aContext );
  }
}

/* EOF */
