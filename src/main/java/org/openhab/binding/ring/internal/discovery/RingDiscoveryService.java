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
package org.openhab.binding.ring.internal.discovery;

import static org.openhab.binding.ring.RingBindingConstants.*;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.ring.internal.RingDeviceRegistry;
import org.openhab.binding.ring.internal.data.RingDevice;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The RingDiscoveryService is responsible for auto detecting a Ring
 * device in the local network.
 *
 * @author Wim Vissers - Initial contribution
 * @author Chris Milbert - Stickupcam contribution
 */

@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.ring")
public class RingDiscoveryService extends AbstractDiscoveryService {

    private Logger logger = LoggerFactory.getLogger(RingDiscoveryService.class);
    private ScheduledFuture<?> discoveryJob;

    private static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS;
    private static final int INTERVAL = 120;

    public static Set<ThingTypeUID> getSupportedTypes() {
        if (SUPPORTED_THING_TYPES_UIDS == null) {
            SUPPORTED_THING_TYPES_UIDS = new HashSet<>();
            SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_ACCOUNT);
            SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_CHIME);
            SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_DOORBELL);
            SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_STICKUPCAM);
        }
        return SUPPORTED_THING_TYPES_UIDS;
    }

    public RingDiscoveryService() {
        super(getSupportedTypes(), 5, true);
    }

    public void activate() {
        logger.debug("Starting Ring discovery...");
        startScan();
        startBackgroundDiscovery();
    }

    @Override
    public void deactivate() {
        logger.debug("Stopping Ring discovery...");
        stopBackgroundDiscovery();
        stopScan();
    }

    private void discover() {
        RingDeviceRegistry registry = RingDeviceRegistry.getInstance();
        for (RingDevice device : registry.getRingDevices(RingDeviceRegistry.Status.ADDED)) {
            thingDiscovered(device.getDiscoveryResult());
            registry.setStatus(device.getId(), RingDeviceRegistry.Status.DISCOVERED);
        }
    }

    @Override
    protected void startBackgroundDiscovery() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    discover();
                } catch (Exception e) {
                    logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                }
            }
        };
        discoveryJob = scheduler.scheduleAtFixedRate(runnable, 0, INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.info("Stop Ring background discovery");
        if (discoveryJob != null && !discoveryJob.isCancelled()) {
            discoveryJob.cancel(true);
            discoveryJob = null;
        }
    }

    @Override
    protected void startScan() {
        logger.debug("Starting device search...");
        try {
            discover();
        } catch (Exception e) {
            logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
        }
    }

    @Override
    protected synchronized void stopScan() {
        removeOlderResults(getTimestampOfLastScan());
        super.stopScan();
        if (!isBackgroundDiscoveryEnabled()) {
        }
    }

}
