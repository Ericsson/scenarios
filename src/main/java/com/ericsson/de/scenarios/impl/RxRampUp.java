/*
 * COPYRIGHT Ericsson (c) 2016.
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 */

package com.ericsson.de.scenarios.impl;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.concurrent.TimeUnit;

public class RxRampUp {
    /**
     * @return Provider for strategy when all vUsers start at once
     */
    public static StrategyProvider allAtOnce() {
        return new StrategyProvider() {
            @Override
            public Strategy provideFor(int vUsers) {
                return new Strategy() {
                    @Override
                    public long nextVUserDelayDelta() {
                        return 0;
                    }
                };
            }
        };
    }

    /**
     * @return Provider for strategy when all vUsers are started during defined time period. For example if 5 vUsers should be
     * started during 10 seconds, new vUser will be started each 10/5 = 2 seconds
     */
    public static StrategyProvider during(final long rampUpTime, final TimeUnit unit) {
        return new StrategyProvider() {
            @Override
            public Strategy provideFor(final int vUsers) {
                final long delay = MILLISECONDS.convert(rampUpTime, unit) / vUsers;
                checkArgument(vUsers != -1, "When using RampUp .withVUsers(int) should be defined for rxFlow");
                checkArgument(delay > 0, "Period to Ramp Up " + vUsers + " vUsers is too short");

                return new Strategy() {
                    int vUser = -1;

                    @Override
                    public long nextVUserDelayDelta() {
                        if (++vUser < vUsers) {
                            return delay;
                        } else {
                            return 0;
                        }
                    }
                };
            }
        };
    }

    /**
     * @return Builder for strategy when defined count of vUsers is started every defined time period. For example if
     * `vUsers(5).every(10, SECONDS)`, every 10 seconds 5 vUsers will start.
     */
    public static VUserStepStrategyBuilder vUsers(int vUsers) {
        return new VUserStepStrategyBuilder(vUsers);
    }

    public static class VUserStepStrategyBuilder {
        private int vUsersPerStep;

        public VUserStepStrategyBuilder(int vUsersPerStep) {
            this.vUsersPerStep = vUsersPerStep;
        }

        public StrategyProvider every(final long timePeriod, final TimeUnit unit) {
            return new StrategyProvider() {
                @Override
                public Strategy provideFor(int vUsers) {
                    checkArgument(vUsers >= vUsersPerStep, "Unable to Ramp Up more vUsers " + "than defined in .withVUsers(int)");

                    return new Strategy() {
                        int vUser = -1;
                        long delayDelta = MILLISECONDS.convert(timePeriod, unit);

                        @Override
                        public long nextVUserDelayDelta() {
                            vUser++;
                            if (vUser != 0 && vUser % vUsersPerStep == 0) {
                                return delayDelta;
                            } else {
                                return 0;
                            }
                        }
                    };
                }
            };
        }
    }

    public interface Strategy {
        long nextVUserDelayDelta();
    }

    public interface StrategyProvider {
        Strategy provideFor(int vUsers);
    }
}
