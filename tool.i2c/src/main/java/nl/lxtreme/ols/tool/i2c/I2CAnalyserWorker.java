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
 * Copyright (C) 2006-2010 Michael Poppitz, www.sump.org
 * Copyright (C) 2010 J.W. Janssen, www.lxtreme.nl
 */
package nl.lxtreme.ols.tool.i2c;


import java.util.logging.*;

import nl.lxtreme.ols.api.*;
import nl.lxtreme.ols.tool.base.*;


/**
 * 
 */
public class I2CAnalyserWorker extends BaseToolWorker<I2CDataSet>
{
  // CONSTANTS

  public static final String  LINE_A                   = "LineA";
  public static final String  LINE_B                   = "LineB";

  public static final String  PROPERTY_AUTO_DETECT_SCL = "AutoDetectSCL";
  public static final String  PROPERTY_AUTO_DETECT_SDA = "AutoDetectSDA";

  private static final Logger LOG                      = Logger.getLogger( I2CAnalyserWorker.class.getName() );

  // VARIABLES

  private final boolean       reportACK;
  private final boolean       reportNACK;
  private final boolean       reportStart;
  private final boolean       reportStop;

  private final int           lineAmask;
  private final int           lineBmask;

  // CONSTRUCTORS

  /**
   * @param aData
   */
  public I2CAnalyserWorker( final CapturedData aData, final I2CProtocolAnalysisDialog aDialog )
  {
    super( aData );

    this.reportACK = aDialog.isReportACK();
    this.reportNACK = aDialog.isReportNACK();
    this.reportStart = aDialog.isReportStart();
    this.reportStop = aDialog.isReportStop();

    this.lineAmask = aDialog.getLineAmask();
    this.lineBmask = aDialog.getLineBmask();
  }

  // METHODS

  /**
   * This is the I2C protocol decoder core
   * The decoder scans for a decode start event when one of the two
   * lines is going low (start condition). After this the
   * decoder starts to decode the data.
   * 
   * @see javax.swing.SwingWorker#doInBackground()
   */
  @Override
  protected I2CDataSet doInBackground() throws Exception
  {
    final CapturedData analysisData = getData();

    // process the captured data and write to output
    int sampleIdx, oldSCL, oldSDA, bitCount;
    int byteValue;
    int sdaMask, sclMask;

    /*
     * Build bitmasks based on the lineA, lineB pins
     * pins.
     */
    final int dataMask = this.lineAmask | this.lineBmask;
    final int sampleCount = analysisData.values.length;

    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "Line A mask = 0x" + Integer.toHexString( this.lineAmask ) );
      LOG.fine( "Line B mask = 0x" + Integer.toHexString( this.lineBmask ) );
    }

    /*
     * first of all scan both lines until they are high (IDLE), then
     * the first line that goes low is the SDA line (START condition).
     */
    for ( sampleIdx = 0; sampleIdx < sampleCount; sampleIdx++ )
    {
      final int dataValue = analysisData.values[sampleIdx];

      if ( ( dataValue & dataMask ) == dataMask )
      {
        // IDLE found here
        break;
      }

      setProgress( ( sampleIdx * 100 / sampleCount ) );
    }

    if ( sampleIdx == sampleCount )
    {
      // no idle state could be found
      if ( LOG.isLoggable( Level.FINE ) )
      {
        LOG.fine( "No IDLE state found in data; aborting analysis..." );
      }
      return null;
    }

    sdaMask = 0;
    sclMask = 0;

    // a is now the start of idle, now find the first start condition
    for ( ; sampleIdx < sampleCount; sampleIdx++ )
    {
      final int dataValue = analysisData.values[sampleIdx];

      if ( ( ( dataValue & dataMask ) != dataMask ) && ( ( dataValue & dataMask ) != 0 ) )
      {
        // one line is low
        if ( ( dataValue & this.lineAmask ) == 0 )
        {
          // lineA is low and lineB is high here: lineA = SDA, lineB = SCL
          sdaMask = this.lineAmask;
          sclMask = this.lineBmask;

          firePropertyChange( PROPERTY_AUTO_DETECT_SCL, null, LINE_B );
          firePropertyChange( PROPERTY_AUTO_DETECT_SDA, null, LINE_A );
        }
        else
        {
          // lineB is low and lineA is high here: lineA = SCL, lineB = SDA
          sdaMask = this.lineBmask;
          sclMask = this.lineAmask;

          firePropertyChange( PROPERTY_AUTO_DETECT_SCL, null, LINE_A );
          firePropertyChange( PROPERTY_AUTO_DETECT_SDA, null, LINE_B );
        }

        break;
      }

      setProgress( ( sampleIdx * 100 / sampleCount ) );
    }

    if ( sampleIdx == sampleCount )
    {
      // no start condition could be found
      if ( LOG.isLoggable( Level.FINE ) )
      {
        LOG.fine( "No START condition found! Analysis aborted..." );
      }
      return null;
    }

    final I2CDataSet i2cDataSet = new I2CDataSet( sampleIdx, sampleCount );
    final long max = sampleCount - sampleIdx;

    // We've just found our start condition, start the report with that...
    reportStartCondition( i2cDataSet, calculateTime( analysisData.timestamps[sampleIdx] ) );

    /*
     * Now decode the bytes, SDA may only change when SCL is low. Otherwise
     * it may be a repeated start condition or stop condition. If the start/stop
     * condition is not at a byte boundary a bus error is detected. So we have to
     * scan for SCL rises and for SDA changes during SCL is high.
     * Each byte is followed by a 9th bit (ACK/NACK).
     */
    oldSCL = analysisData.values[sampleIdx] & sclMask;
    oldSDA = analysisData.values[sampleIdx] & sdaMask;
    bitCount = 8;
    byteValue = 0;

    for ( sampleIdx = i2cDataSet.getStartSampleIndex(); sampleIdx < i2cDataSet.getStopSampleIndex() - 1; sampleIdx++ )
    {
      final long time = calculateTime( analysisData.timestamps[sampleIdx] );
      final int dataValue = analysisData.values[sampleIdx];

      final int sda = dataValue & sdaMask;
      final int scl = dataValue & sclMask;

      // detect SCL rise
      if ( scl > oldSCL )
      {
        // SCL rises
        if ( sda != oldSDA )
        {
          reportBusError( i2cDataSet, time );
        }
        else
        {
          // read SDA
          if ( bitCount == 0 )
          {
            // read the confirmation of the slave...
            if ( sda != 0 )
            {
              // NACK
              reportNACK( i2cDataSet, time );
            }
            else
            {
              // ACK
              reportACK( i2cDataSet, time );
            }

            // next byte
            bitCount = 8;
          }
          else
          {
            bitCount--;
            if ( sda != 0 )
            {
              byteValue |= ( 1 << bitCount );
            }

            if ( bitCount == 0 )
            {
              // store decoded byte
              reportData( i2cDataSet, time, byteValue );
              byteValue = 0;
            }
          }
        }
      }

      // detect SDA change when SCL high
      if ( ( scl == sclMask ) && ( sda != oldSDA ) )
      {
        // SDA changes here
        if ( bitCount < 7 )
        {
          // bus error, no complete byte detected
          reportBusError( i2cDataSet, time );
        }
        else
        {
          if ( sda > oldSDA )
          {
            // SDA rises, this is a stop condition
            reportStopCondition( i2cDataSet, time );
          }
          else
          {
            // SDA falls, this is a start condition
            reportStartCondition( i2cDataSet, time );
          }
          // new byte
          bitCount = 8;
        }
      }

      oldSCL = scl;
      oldSDA = sda;

      setProgress( ( int )Math.max( 0.0, Math.min( 100.0, ( sampleIdx - i2cDataSet.getStartSampleIndex() ) * 100.0
          / max ) ) );
    }

    return i2cDataSet;
  }

  /**
   * calculate the time offset
   * 
   * @param time
   *          absolute sample number
   * @return time relative to data
   */
  private long calculateTime( final long time )
  {
    if ( getData().hasTriggerData() )
    {
      return time - getData().triggerPosition;
    }
    else
    {
      return time;
    }
  }

  /**
   * @param aTime
   */
  private void reportACK( final I2CDataSet aDataSet, final long aTime )
  {
    if ( this.reportACK )
    {
      aDataSet.reportACK( aTime );
    }
  }

  /**
   * @param aTime
   */
  private void reportBusError( final I2CDataSet aDataSet, final long aTime )
  {
    aDataSet.reportBusError( aTime );
  }

  /**
   * @param aTime
   * @param aByteValue
   */
  private void reportData( final I2CDataSet aDataSet, final long aTime, final int aByteValue )
  {
    aDataSet.reportData( aTime, aByteValue );
  }

  /**
   * @param aDataSet
   * @param aTime
   */
  private void reportNACK( final I2CDataSet aDataSet, final long aTime )
  {
    if ( this.reportNACK )
    {
      aDataSet.reportNACK( aTime );
    }
  }

  /**
   * @param aTime
   */
  private void reportStartCondition( final I2CDataSet aDataSet, final long aTime )
  {
    if ( this.reportStart )
    {
      aDataSet.reportStartCondition( aTime );
    }
  }

  /**
   * @param aTime
   */
  private void reportStopCondition( final I2CDataSet aDataSet, final long aTime )
  {
    if ( this.reportStop )
    {
      aDataSet.reportStopCondition( aTime );
    }
  }

}

/* EOF */