package cn.com.hnisi.util;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class XmlUtil {
	/**
	 * 根据file文件，获取expression的节点文本内容
	 * 
	 * @param file
	 *            c:\test.xml
	 * @param expression
	 *            例：expression = "/domain/app-deployment[attribute='12']/target"
	 * @return
	 */
	public static String getNodeText(File file, String expression) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();// 创建一个																			// DocumentBuilderFactory实例
		try {
			DocumentBuilder db = dbf.newDocumentBuilder();// 创建DocumentBuilder实例
			Document doc = db.parse(file);
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			Node node = (Node) xpath.evaluate(expression, doc,
					XPathConstants.NODE);
			if (node != null) {
				return node.getTextContent();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
