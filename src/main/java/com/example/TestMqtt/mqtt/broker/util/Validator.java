package com.example.TestMqtt.mqtt.broker.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

public class Validator {
    private static String clientId = "^[ -~]+$";
    private static String userName = "^[ -~]+$";
    private static String password = "^[ -~]+$";
    private static String topicName = "^[ -~]+$";
    private static String topicFilter = "^[ -~]+$";

    // MQTT client id validate regex pattern
    protected Pattern clientIdPattern;
    // MQTT user name validate regex pattern
    protected Pattern userNamePattern;
    // MQTT password validate regex pattern
    protected Pattern passwordPattern;
    // MQTT topic name validate regex pattern
    protected Pattern topicNamePattern;
    // MQTT topic filter validate regex pattern
    protected Pattern topicFilterPattern;

    public Validator() {

        this.clientIdPattern = Pattern.compile(clientId);

        this.userNamePattern = Pattern.compile(userName);

        this.passwordPattern = Pattern.compile(password);

        this.topicNamePattern = Pattern.compile(topicName);

        this.topicFilterPattern = Pattern.compile(topicFilter);
    }

    /**
     * Is MQTT topic name (no wildcards) valid
     *
     * @param topicName Topic Name
     * @return True if valid
     */
    public boolean isTopicNameValid(String topicName) {
        return !StringUtils.isEmpty(topicName)
                && !topicName.contains("+")
                && !topicName.contains("#")
                && (this.topicNamePattern == null || this.topicNamePattern.matcher(topicName).matches());
    }

    /**
     * Is MQTT topic filter (may contain wildcards) valid
     *
     * @param topicFilter Topic Filter
     * @return True if valid
     */
    public boolean isTopicFilterValid(String topicFilter) {
        return !StringUtils.isEmpty(topicFilter)
                && (this.topicFilterPattern == null
                || this.topicFilterPattern.matcher(topicFilter).matches());
    }

    /**
     * Is MQTT client id valid
     *
     * @param clientId Client Id
     * @return True if valid
     */
    public boolean isClientIdValid(String clientId) {
        return this.clientIdPattern == null || this.clientIdPattern.matcher(clientId).matches();
    }

    /**
     * Is MQTT user name valid
     *
     * @param userName User Name
     * @return True if valid
     */
    public boolean isUserNameValid(String userName) {
        return this.userNamePattern == null || this.userNamePattern.matcher(userName).matches();
    }

    /**
     * Is MQTT password valid
     *
     * @param password Password
     * @return True if valid
     */
    public boolean isPasswordValid(String password) {
        return this.passwordPattern == null || this.passwordPattern.matcher(password).matches();
    }
}
