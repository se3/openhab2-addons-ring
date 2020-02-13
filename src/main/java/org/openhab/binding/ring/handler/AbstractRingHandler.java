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

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.openhab.binding.ring.internal.RingDeviceRegistry;
import org.openhab.binding.ring.internal.errors.DeviceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractRingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Wim Vissers - Initial contribution
 */

public abstract class AbstractRingHandler extends BaseThingHandler {

    // Current status
    protected OnOffType status;
    protected OnOffType enabled;
    protected Logger logger = LoggerFactory.getLogger(AbstractRingHandler.class);

    // Scheduler
    ScheduledFuture<?> refreshJob;

    /**
     * There is no default constructor. We have to define a
     * constructor with Thing object as parameter.
     *
     * @param thing
     */
    public AbstractRingHandler(final Thing thing) {
        super(thing);
        status = OnOffType.OFF;
        enabled = OnOffType.ON;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing AbstractRingHandler");
        Map<String, String> properties = editProperties();
        properties.put(Thing.PROPERTY_SERIAL_NUMBER, getThing().getUID().getId());
        updateProperties(properties);
        // super.initialize();
    }

    /**
     * Refresh the state of channels that may have changed by (re-)initialization.
     */
    protected abstract void refreshState();

    /**
     * Called every minute
     */
    protected abstract void minuteTick();

    /**
     * Check every 60 seconds if one of the alarm times is reached.
     */
    protected void startAutomaticRefresh(final int refreshInterval) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    minuteTick();
                } catch (final Exception e) {
                    logger.debug("Exception occurred during execution of startAutomaticRefresh(): {}", e.getMessage(),
                            e);
                }
            }
        };

        refreshJob = scheduler.scheduleWithFixedDelay(runnable, 0, refreshInterval, TimeUnit.SECONDS);
        refreshState();
    }

    protected void stopAutomaticRefresh() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
    }

    /**
     * Dispose off the refreshJob nicely.
     */
    @Override
    public void dispose() {
        stopAutomaticRefresh();
    }

    @Override
    public void handleRemoval() {
        updateStatus(ThingStatus.REMOVING);
        final String id = getThing().getUID().getId();
        final RingDeviceRegistry registry = RingDeviceRegistry.getInstance();
        try {
            registry.removeRingDevice(id);
        } catch (final DeviceNotFoundException e) {
            // TODO Auto-generated catch block
            logger.debug("Exception occurred during execution of handleRemoval(): {}", e.getMessage(), e);
        } finally {
            updateStatus(ThingStatus.REMOVED);
        }
    }

}
