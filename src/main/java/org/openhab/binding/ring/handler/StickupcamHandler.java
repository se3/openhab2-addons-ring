/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.ring.handler;

import static org.openhab.binding.ring.RingBindingConstants.CHANNEL_STATUS_BATTERY;

import java.math.BigDecimal;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.ring.internal.RingDeviceRegistry;
import org.openhab.binding.ring.internal.data.Stickupcam;
import org.openhab.binding.ring.internal.errors.DeviceNotFoundException;
import org.openhab.binding.ring.internal.errors.IllegalDeviceClassException;

/**
 * The handler for a Ring Video Stickup Cam.
 *
 * @author Chris Milbert - Initial contribution
 *
 */

public class StickupcamHandler extends RingDeviceHandler {
    private Integer lastBattery = -1;

    public StickupcamHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Stickupcam handler");
        super.initialize();

        // Configuration config = getThing().getConfiguration();

        RingDeviceRegistry registry = RingDeviceRegistry.getInstance();
        String id = getThing().getUID().getId();
        if (registry.isInitialized()) {
            try {
                linkDevice(id, Stickupcam.class);
                updateStatus(ThingStatus.ONLINE);
            } catch (DeviceNotFoundException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Device with id '" + id + "' not found");
            } catch (IllegalDeviceClassException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Device with id '" + id + "' of wrong type");
            }
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                    "Waiting for RingAccount to initialize");
        }

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
        if (this.refreshJob == null) {
            Configuration config = getThing().getConfiguration();
            Integer refreshInterval = ((BigDecimal) config.get("refreshInterval")).intValueExact();
            startAutomaticRefresh(refreshInterval);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void refreshState() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void minuteTick() {
        if (device == null) {
            initialize();
        }

        if ((device != null) && (!device.getBattery().equals(lastBattery))) {
            ChannelUID channelUID = new ChannelUID(thing.getUID(), CHANNEL_STATUS_BATTERY);
            updateState(channelUID, new DecimalType(device.getBattery()));
            lastBattery = device.getBattery();
        }
    }

}
