package com.zwl.config.impls;

/**
 * @author 曾文亮
 * @version 1.0.0
 * @email wenliang_zeng416@163.com
 * @date 2024年03月18日 19:58:07
 * @packageName com.zwl.config.impls
 * @className ConfigCenterImpl
 * @describe TODO
 */
public interface ConfigCenterImpl {
    /**
     * 更新配置
     * @param content
     */
    public void updateApusicConfigs(String content);

    /**
     * 发布配置
     * @return
     */
    public boolean publishApusicConfig();

    /**
     * 监听配置
     * @throws Exception
     */
    public void addConfigListener() throws Exception;
}
