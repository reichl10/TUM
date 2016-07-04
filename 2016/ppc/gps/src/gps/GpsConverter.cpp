/**
 * \copyright
 * (c) 2012 - 2015 E.S.R. Labs GmbH (http://www.esrlabs.com)
 * All rights reserved.
 */

#include "gps/GpsConverter.h"
#include "ac/IGpsACPusher.h"
#include "can/canframes/CANFrame.h"
#include "can/ICANTransceiver.h"
#include <stdio.h>
#include <math.h>

#define invalidVal 0x80000000
#define noSig1 0x7FFFFFFF
#define noSig2 0xFFFFFFFF

using namespace can;

namespace gps
{

GpsConverter::GpsConverter(
		ICANTransceiver& transceiver,
		IGpsACPusher& acPusher)
	: fCanTransceiver(transceiver)
	, fCanFilter(GPS_FRAME_ID, GPS_FRAME_ID)
	, fAcPusher(acPusher)
{
}

void GpsConverter::frameReceived(const CANFrame& canFrame)
{
	const uint8* payload = canFrame.getPayload();
    // TOOD implement conversion to arc-msec and call IGpsACPusher
	sint32 latitude_raw = 0;
	sint32 longitude_raw = 0;

	latitude_raw = payload[7] << 24 | payload[6] << 16 | payload[5] << 8 | payload[4];
	longitude_raw = payload[3] << 24 | payload[2] << 16 | payload[1] << 8 | payload[0];
	
	if (latitude_raw != invalidVal && latitude_raw != noSig1 && latitude_raw != noSig1 &&
		longitude_raw != invalidVal && longitude_raw != noSig1 && longitude_raw != noSig1) {

		sint32 latInMs = ((180/((pow(2, 31))-1))*latitude_raw)*3600*1000;
		sint32 longInMs = ((180*longitude_raw)/((pow(2, 31))-1)) *3600*1000;



		if (latInMs != fLastLatInMs || longInMs != fLastLongInMs)
		{
			// value changed
			fAcPusher.pushGPSCoordinates(latInMs, longInMs);
			fLastLatInMs = latInMs;
			fLastLongInMs = longInMs;
		}
	}
}


} // namespace gps


