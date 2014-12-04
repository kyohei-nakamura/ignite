/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.visor.streamer;

import org.apache.ignite.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.*;
import org.gridgain.grid.util.typedef.internal.*;

import static org.gridgain.grid.kernal.visor.util.VisorTaskUtils.*;

/**
 * Task for reset specified streamer.
 */
@GridInternal
public class VisorStreamerResetTask extends VisorOneNodeTask<String, Void> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorStreamerResetJob job(String arg) {
        return new VisorStreamerResetJob(arg);
    }

    /**
     * Job that reset streamer.
     */
    private static class VisorStreamerResetJob extends VisorJob<String, Void> {
        /** */
        private static final long serialVersionUID = 0L;

        /**
         * @param arg Streamer name.
         */
        private VisorStreamerResetJob(String arg) {
            super(arg);
        }

        /** {@inheritDoc} */
        @Override protected Void run(String streamerName) throws GridException {
            try {
                IgniteStreamer streamer = g.streamer(streamerName);

                streamer.reset();

                return null;
            }
            catch (IllegalArgumentException iae) {
                throw new GridException("Failed to reset streamer: " + escapeName(streamerName)
                    + " on node: " + g.localNode().id(), iae);
            }
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorStreamerResetJob.class, this);
        }
    }
}
