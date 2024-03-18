package com.zwl.config.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 曾文亮
 * @version 1.0.0
 * @email wenliang_zeng416@163.com
 * @date 2024年03月18日 19:59:08
 * @packageName com.zwl.config.utils
 * @className ConfigCenterUtils
 * @describe TODO
 */
public class ConfigCenterUtils {
    private static final Logger logger = Logger.getLogger(ConfigCenterUtils.class.getName());

    /**
     * 将XML Document对象转换为字符串格式。
     * @param doc XML Document对象，代表一个XML文档。
     * @return 转换后的XML字符串。如果转换过程中出现异常，则返回null。
     */
    public static String convertXMLDocumentToString(Document doc) {
        TransformerFactory tf = TransformerFactory.newInstance();
        try {
            Transformer transformer = tf.newTransformer();
            // 设置输出属性
            transformer.setOutputProperty(OutputKeys.METHOD, "xml"); // 指定输出为XML格式
            transformer.setOutputProperty(OutputKeys.INDENT, "no"); // 不进行缩进
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); // 指定编码为UTF-8
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); // 不输出XML声明

            // 使用StringWriter进行转换并获取结果
            try (StringWriter writer = new StringWriter()) {
                transformer.transform(new DOMSource(doc), new StreamResult(writer));
                String output = writer.toString();
                // 如果结果字符串不以XML声明开始，则添加XML声明
                if (!output.startsWith("<?xml")) {
                    String xmlDeclaration = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
                    output = xmlDeclaration + output;
                }
                return output;
            }
        } catch (Exception e) {
            // 记录转换失败的警告
            logger.warning("The xml file failed to convert the string !");
            return null;
        }
    }
    /**
     * 从配置信息中提取保存配置文件的名称。
     * @param config 包含配置信息的字符串，预期包含一个或多个config标签，每个config标签应包含name属性。
     * @return 返回第一个匹配的config标签的name属性值。如果没有找到匹配的name属性，则返回null。
     */
    public static String getSaveConfigFile(String config) {
        // 编译正则表达式，用于匹配config标签的name属性
        Pattern pattern = Pattern.compile("config\\s+name=\"([^\"]+)\"");
        // 创建Matcher对象，用于在给定的配置信息中查找匹配项
        Matcher matcher = pattern.matcher(config);
        String fileName = null;
        // 遍历配置信息中的所有匹配项
        while (matcher.find()) {
            // 提取并保存第一个匹配的文件名
            fileName = matcher.group(1);
            // 由于我们只提取第一个匹配项，这里可以跳出循环，优化效率
            break;
        }
        return fileName;
    }
    /**
     * 获取文件的完整路径。
     * @param root 文件所在的根目录。
     * @param name 文件名。
     * @return 给定根目录和文件名拼接成的完整路径。
     * @throws RuntimeException 如果系统属性'apusic.home'未设置。
     */
    public static String getFilePath(String root, String name) {
        // 如果根目录未指定，则使用当前目录
        if (root == null) {
            root = System.getProperty("user.dir");
        }
        // 将路径中的斜杠统一转换为系统分隔符
        if (File.separatorChar != '/') {
            name = name.replace('/', File.separatorChar);
            root = root.replace('/', File.separatorChar);
        }
        // 获取系统属性中'apusic.home'的值，作为基础路径
        String path = System.getProperty("apusic.home");

        // 如果'apusic.home'未设置，则抛出异常
        if (path == null) {
            throw new RuntimeException("Did not set system property 'apusic.home'");
        }

        // 拼接最终的文件路径并返回
        return path + File.separatorChar + root + File.separatorChar + name;
    }

    /**
     * 保存配置信息到相应的配置文件中。
     *
     * @param config 包含配置信息的字符串，一般为XML格式。
     * @param configsFileName 配置文件的名称，不包含路径和扩展名。
     */
    public static void saveConfigs(String config, String configsFileName) {
        // 更新configs.xml文件
        writeXMLFile(getFilePath("conf", configsFileName), config);

        // 使用正则表达式匹配<config>标签内的内容
        Pattern pattern = Pattern.compile("<config [^>]*>(.*?)</config>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(config);

        while (matcher.find()) {
            // 捕获到的完整的<config>标签内容
            String apusicConfigItem = matcher.group();
            // 解析出配置项将要保存的文件名
            String fileName = getSaveConfigFile(apusicConfigItem);
            // 解析出<config>标签内的具体配置项内容
            String configItem = matcher.group(1);

            // 专门处理apusic.conf配置项
            if ("apusic.conf".equals(fileName)) {
                saveApusicConfig(apusicConfigItem, getFilePath("conf", fileName));
                continue;
            }
            // 如果文件名不是以properties结尾，则在配置项前添加XML声明
            if (!fileName.endsWith("properties")) {
                configItem = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + configItem;
                writeXMLFile(getFilePath("conf", fileName), configItem);
            }
            // 保存配置项到对应的文件中
            writeXMLFile(getFilePath("conf", fileName), configItem);
            // 记录保存成功的日志信息
            logger.info("File is saved in " + getFilePath("conf", fileName));
        }
    }

    /**
     * 从指定路径获取 XML 配置文件的内容。
     *
     * @param path 相对于 conf 目录的文件路径
     * @return XML 配置文件的内容，如果文件不存在或读取失败则返回 null
     */
    public static String getXMLConfig(String path) {
        // 获取配置文件的完整路径
        String filePath = ConfigCenterUtils.getFilePath("conf", path);
        // 检查文件是否存在
        if (!Files.exists(Paths.get(filePath))) {
            logger.warning("The file " + filePath + " does not exist.");
            return null;
        }

        // 创建 DocumentBuilderFactory 实例，并设置属性以禁用外部实体，防止 XXE 攻击
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        try (InputStream is = Files.newInputStream(Paths.get(filePath))) {
            // 创建 DocumentBuilder 并解析 XML 文件
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(is);
            // 标准化 XML 文档结构
            document.getDocumentElement().normalize();
            // 将解析后的 Document 对象转换为字符串并返回
            return ConfigCenterUtils.convertXMLDocumentToString(document);
        } catch (Exception e) {
            logger.warning("Failed to parse the XML file: " + e.getMessage());
        }

        return null;
    }

    /**
     * 将指定的内容写入XML文件。
     *
     * @param filePath 要写入的文件路径。
     * @param content 要写入文件的XML内容。
     */
    private static void writeXMLFile(String filePath, String content) {
        File file = new File(filePath);
        try (FileWriter writer = new FileWriter(file)) {
            // 将提供的XML内容写入指定的文件
            writer.write(content);
            writer.flush();   // 确保内容被刷新到文件中
        } catch (IOException e) {
            // 处理可能的输入/输出异常
            e.printStackTrace();
        }
    }

    /**
     * 保存 Apusic 配置文件。
     * 该方法将给定的 Apusic 配置字符串解析为 XML 文档，移除所有 'config' 元素中的 'name' 属性，
     * 然后将处理后的 XML 内容写入指定的文件路径。
     *
     * @param apusicConfig Apusic 配置的字符串表示。
     * @param filePath 要保存配置文件的路径。
     */
    private static void saveApusicConfig(String apusicConfig, String filePath) {
        try {
            // 初始化 XML 解析器
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            // 解析配置字符串为 XML 文档并标准化文档结构
            Document document = builder.parse(new ByteArrayInputStream(apusicConfig.getBytes("UTF-8")));
            document.getDocumentElement().normalize();

            // 移除 'config' 元素中的 'name' 属性
            NodeList configList = document.getElementsByTagName("config");
            for (int i = 0; i < configList.getLength(); i++) {
                ((Element) configList.item(i)).removeAttribute("name");
            }

            // 创建新 Document 用于保存处理后的 XML 内容
            Document newDocument = builder.newDocument();
            // 配置 XML 转换器
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            // 构建最终的 XML 内容
            String xmlDeclaration = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
            StringBuilder xmlContentBuilder = new StringBuilder(xmlDeclaration);

            // 将处理过的 'config' 节点逐一复制到新 Document 并转换为字符串
            for (int i = 0; i < configList.getLength(); i++) {
                Node importedConfig = newDocument.importNode(configList.item(i), true);
                newDocument.appendChild(importedConfig);

                StringWriter writer = new StringWriter();
                try {
                    transformer.transform(new DOMSource(newDocument), new StreamResult(writer));
                    xmlContentBuilder.append(writer.toString());
                    // 清理当前的文档以备下一个节点处理
                    newDocument.removeChild(importedConfig);
                } finally {
                    writer.close(); // 确保资源被关闭
                }
            }

            // 将最终的 XML 内容写入文件
            try (FileWriter fileWriter = new FileWriter(new File(filePath))) {
                fileWriter.write(xmlContentBuilder.toString());
            }
            logger.info("File is saved in " + filePath);

        } catch (Exception e) {
            logger.warning("An error occurred while saving the Apusic config.");
        }
    }
}
