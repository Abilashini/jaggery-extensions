/*
 *  Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jaggeryjs.modules.sso.common.clustering;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.ClusteringMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jaggeryjs.modules.sso.common.constants.SSOConstants;

/**
 * Responsible for attempting to retransmit a failed clustering message.This class will
 * attempt resend the message a set amount of time (retryCount) with a predefined wait between each
 * attempt.The attempts to send the message will cease once either the retry count has been met or
 * or the message is transmitted successfully (which ever happens first).
 */
public class FailedClusterMessageTransmitter implements Runnable {
    private static final Log log = LogFactory.getLog(FailedClusterMessageTransmitter.class);
    private ClusteringAgent agent;
    private ClusteringMessage message;
    private int retryCount;

    public FailedClusterMessageTransmitter(ClusteringAgent agent, ClusteringMessage message) {
        this.agent = agent;
        this.retryCount = 0;
        this.message = message;
    }

    @Override
    public void run() {
        boolean isSuccess = false;
        try {
            //Continue till either the retry count has been reached or till
            //message is successfully transmitted to the other cluster nodes
            while ((!isSuccess) && (retryCount < SSOConstants.MAX_CLUSTER_MESSAGE_RETRY_COUNT)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Attempting to retransmit message : %s ", message));
                }
                isSuccess = send();
                retryCount++;
                Thread.sleep(SSOConstants.CLUSTERING_MESSAGE_RETRY_DELAY);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        //Determine if the message was sent
        if (!isSuccess) {
            log.error("Permanently failed to send clustering message " + message + " since max retry count has been " +
                    "reached.There will not be any further attempts to send this message.");
        }
    }

    private boolean send() {
        boolean isSuccess = false;
        try {
            agent.sendMessage(message, SSOConstants.CLUSTERING_MESSAGE_ISRPC);
            isSuccess = true;
        } catch (ClusteringFault e) {
            log.error("Failed to send message " + message + ". Retry count at " + retryCount, e);
        }
        return isSuccess;
    }
}
