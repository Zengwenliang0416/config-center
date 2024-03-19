package com.zwl.config;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.zwl.config.impls.ConfigCenterImpl;
import com.zwl.config.utils.ConfigCenterUtils;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * @author 曾文亮
 * @version 1.0.0
 * @email wenliang_zeng416@163.com
 * @date 2024年03月18日 19:58:38
 * @packageName com.zwl.config
 * @className ConfigCenter
 * @describe 配置中心实现类
 */
public class ConfigCenter implements ConfigCenterImpl {
    private static final Logger logger = Logger.getLogger(ConfigCenter.class.getName());
    private final String configCenterAddr;
    private final String dataId;
    private final String group;
    private final long timeoutMs;
    private final String configsFileName;
    private final Listener listener;

    /**
     * 配置中心的单例模式实现。
     * 这是一个私有静态内部类，用于封装配置中心的单例实例。
     */
    private static class Holder {
        // 配置中心的单例实例。
        private static final ConfigCenter INSTANCE = new ConfigCenter();
    }

    /**
     * 获取ConfigCenter的单例实例。
     *
     * @return ConfigCenter的单例实例。
     */
    public static ConfigCenter getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * 获取配置值，优先从环境变量中获取，如果环境变量中不存在，则尝试从系统属性中获取。
     *
     * @param envKey     环境变量的键，用于尝试从环境变量中获取配置值。
     * @param sysPropKey 系统属性的键，当环境变量中未找到指定键的值时，用于尝试从系统属性中获取配置值。
     * @return 返回配置值，如果既未在环境变量中找到值，也未在系统属性中找到值，则返回null。
     */
    private String getConfigValue(String envKey, String sysPropKey) {
        // 尝试从环境变量中获取配置值
        String value = System.getenv(envKey);
        // 如果环境变量中存在指定键的值，则返回该值；否则尝试从系统属性中获取。
        return value != null ? value : System.getProperty(sysPropKey);
    }

    /**
     * 获取配置值 - 先尝试从环境变量中获取指定键的值，如果未找到，则尝试从系统属性中获取。
     *
     * @param envKey       环境变量的键，用于首先查找配置值。
     * @param sysPropKey   系统属性的键，如果环境变量中未找到指定键的值，则尝试查找此键对应的系统属性值。
     * @param defaultValue 如果既未找到环境变量中的值，也未找到系统属性中的值，则返回此默认值。
     * @return 指定键的值，优先从环境变量获取，若未找到则从系统属性获取，如果都未找到则返回默认值。
     */
    private String getConfigValue(String envKey, String sysPropKey, String defaultValue) {
        // 尝试从环境变量中获取值
        String value = System.getenv(envKey);
        // 如果环境变量中未找到值，则尝试从系统属性中获取，同时提供默认值
        return value != null ? value : System.getProperty(sysPropKey, defaultValue);
    }


    /**
     * ConfigCenter的私有构造方法，用于初始化配置中心的相关属性。
     * 该构造方法不接受参数，并且没有返回值。
     * 在构造过程中，会从配置中读取以下信息：
     * 1. 配置中心地址（CONFIG_CENTER_ADDR）
     * 2. 配置数据ID（CONFIG_DATA_ID）
     * 3. 配置分组（CONFIG_GROUP，默认为DEFAULT_GROUP）
     * 4. 配置超时时间（CONFIG_TIMEOUT_MS，默认为3000毫秒）
     * 5. 配置文件名称（CONFIG_FILE_NAME，默认为configs.xml）
     * 同时，会创建一个监听器并初始化日志信息。
     */
    private ConfigCenter() {
        // 初始化配置中心地址，必须指定
        this.configCenterAddr = getConfigValue("CONFIG_CENTER_ADDR", "CONFIG_CENTER_ADDR");
        // 初始化配置数据ID，从configs.xml文件中读取，如果不存在则使用默认值
        this.dataId = getConfigValue("CONFIG_DATA_ID", "CONFIG_DATA_ID", "configs.xml");
        // 初始化配置分组，默认使用DEFAULT_GROUP
        this.group = getConfigValue("CONFIG_GROUP", "CONFIG_GROUP", "DEFAULT_GROUP");
        // 读取配置超时时间，以毫秒为单位，默认为3000
        this.timeoutMs = Long.parseLong(getConfigValue("CONFIG_TIMEOUT_MS", "CONFIG_TIMEOUT_MS", "3000"));
        // 初始化配置文件名称，默认为configs.xml
        this.configsFileName = getConfigValue("CONFIG_FILE_NAME", "CONFIG_FILE_NAME", "configs.xml");
        // 创建并初始化监听器，用于接收配置信息更新
        this.listener = new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                // 当接收到配置信息更新时，更新应用的配置
                updateApusicConfigs(configInfo);
            }

            @Override
            public Executor getExecutor() {
                // 返回null，表示不使用异步处理。可根据需要提供一个执行器。
                return null;
            }
        };
        // 记录配置中心客户端初始化信息
        logger.info("ConfigCenter client has been initialized.");
    }

    /**
     * 更新Apusic配置文件。
     * 该方法将指定的配置内容保存到Apusic的配置文件中。
     *
     * @param content 配置内容的字符串表示。此字符串应包含需要更新的所有配置信息。
     */
    public void updateApusicConfigs(String content) {
        // 使用ConfigCenterUtils的saveConfigs方法保存配置内容到指定的配置文件中
        ConfigCenterUtils.saveConfigs(content, configsFileName);
    }

    /**
     * 发布配置到配置中心
     * <p>
     * 本函数尝试将特定的配置数据发布到配置中心。如果发布成功，会记录一条严重的日志信息并返回true；反之，返回false。
     *
     * @return boolean - 如果配置成功发布到配置中心，则返回true；否则返回false。
     */
    public boolean publishApusicConfig() {
        // 尝试发布配置到配置中心
        if (publishApusicConfig(dataId, group)) {
            logger.info("Successfully publish the configuration to the configuration center !");
            return true;
        } else {
            return false;
        }
    }

    /**
     * 发布配置到Apusic配置中心。
     *
     * @param dataId 配置的数据ID，用于唯一标识配置信息。
     * @param group  配置的分组，用于对配置进行分类管理。
     * @return boolean 发布成功返回true，失败返回false。
     */
    public boolean publishApusicConfig(String dataId, String group) {
        if (configCenterAddr == null) {
            logger.severe("Configuration center address not set.");
            return false;
        }
        try {
            // 初始化配置属性，包括配置中心地址
            Properties properties = new Properties();
            properties.put("serverAddr", configCenterAddr);

            // 创建ConfigService实例，用于与配置中心交互
            ConfigService configService = NacosFactory.createConfigService(properties);

            // 从指定文件获取要发布的配置内容
            String content = ConfigCenterUtils.getXMLConfig(configsFileName);

            // 若配置内容为空，则记录警告信息并返回false
            if (content == null) {
                logger.severe("The content to be published is null.");
                return false;
            }

            // 发布配置到配置中心
            return configService.publishConfig(dataId, group, content);

        } catch (Exception e) {
            // 配置发布失败，记录严重错误信息并返回false
            logger.severe("Failed to publish configuration to ConfigCenter: " + e.getMessage());
            return false;
        }
    }

    /**
     * 添加配置监听器。
     * 该方法会将指定的配置数据ID、分组和监听器添加到监听器列表中，以便于当配置发生变化时能够收到通知。
     *
     * @throws Exception 如果添加监听器过程中出现错误，则抛出异常。
     */
    public void addConfigListener() throws Exception {
        addConfigListener(dataId, group, listener);
    }

    /**
     * 添加配置监听器。
     * 该方法用于监听指定数据ID和分组下的配置变更，当配置发生变更时，会调用监听器中的回调方法。
     *
     * @param dataId   配置的数据ID，用于唯一标识配置信息。
     * @param group    配置的分组，用于对配置进行分类管理。
     * @param listener 监听器对象，配置变更时会触发该监听器的回调方法。
     * @throws Exception 如果操作过程中出现异常，则抛出Exception。
     */
    private void addConfigListener(String dataId, String group, Listener listener) throws Exception {
        if (configCenterAddr == null) {
            logger.severe("Configuration center address not set.");
            return;
        }
        // 初始化配置属性，设置配置中心的地址。
        Properties properties = new Properties();
        properties.put("serverAddr", configCenterAddr);

        // 创建配置服务实例，用于与配置中心交互。
        ConfigService configService = NacosFactory.createConfigService(properties);
        String content = configService.getConfig(dataId, group, timeoutMs);
        // 首次加载配置内容。
        logger.info("Listening configuration......");

        // 注册监听器，以便在配置变更时得到通知。
        configService.addListener(dataId, group, listener);
    }
}
