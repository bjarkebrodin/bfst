package dankmap.drawing;

import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static dankmap.util.ColorUtil.*;

public class DrawTypeXMLReader {
    private final DrawTypeMap drawTypeMap = new DrawTypeMap();
    private final Set<Byte> ids = new HashSet<>();
    private final DocumentBuilder builder;
    private final InputStream file;


    public DrawTypeXMLReader(InputStream file) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        builder = factory.newDocumentBuilder();
        this.file = file;
    }

    public DrawTypeMap load(DrawType.Style style) throws IOException, SAXException {
        return loadXML(style);
    }


    private DrawTypeMap loadXML(DrawType.Style style) throws IOException, SAXException {
        Document document = builder.parse(file);
        Element root = document.getDocumentElement();
        root.normalize();

        NodeList nodes = document.getElementsByTagName("draw-type");
        for (int i = 0; i < nodes.getLength(); i++) {
            mapNodeToDrawType(nodes.item(i), style);
        }
        return drawTypeMap;
    }

    private void mapNodeToDrawType(Node node, DrawType.Style style) {
        var builder = new DrawType.Builder();
        if (node.hasAttributes()) {
            Element element = (Element) node;
            var reader = new XMLAttributeReader(element.getAttributes());

                if (reader.hasAttribute("id")) {
                byte id = Byte.parseByte(reader.getAttribute("id"));
                // Just a reminder if multiple drawTypes have same id
                if (ids.contains(id)) {
                    System.out.println("MULTIPLE DRAWTYPE IDS");
                }

                ids.add(id);
                builder.id(id);
            }

            if (reader.hasAttribute("zoomLevel"))
                builder.zoomLevel(Byte.parseByte(reader.getAttribute("zoomLevel")));

            if (reader.hasAttribute("fillColor")) {
                switch (style) {
                    case NORMAL: {
                        builder.fillColor(hexToColor(reader.getAttribute("fillColor")));
                        break;
                    }
                    case DARK: {
                        // Didnt like orange water
                        var hexColor = reader.getAttribute("fillColor");
                        if (hexColor.contains("AAD3DF")) {
                            builder.fillColor(hexToColor("#011a36"));
                        } else {
                            builder.fillColor(darkMode(reader.getAttribute("fillColor")));
                        }
                        break;
                    }
                    case GRAYSCALE: {
                        builder.fillColor(grayScale(reader.getAttribute("fillColor")));
                        break;
                    }
                    case RANDOM: {
                        builder.fillColor(randomRGB());
                        break;
                    }

                    case COLORBLIND: {
                        builder.fillColor(saturatedColor(reader.getAttribute("fillColor")));
                        break;
                    }
                }
            }

            if (reader.hasAttribute("strokeColor")) {
                switch (style) {
                    case NORMAL: {
                        builder.strokeColor(hexToColor(reader.getAttribute("strokeColor")));
                        break;
                    }
                    case DARK: {
                        builder.strokeColor(darkMode(reader.getAttribute("strokeColor")));
                        break;
                    }
                    case GRAYSCALE: {
                        builder.strokeColor(grayScale(reader.getAttribute("strokeColor")));
                        break;
                    }
                    case RANDOM: {
                        builder.strokeColor(randomRGB());
                        break;
                    }
                    case COLORBLIND: {
                        builder.strokeColor(saturatedColor(reader.getAttribute("strokeColor")));
                        break;
                    }
                }
            }

            if (reader.hasAttribute("strokeWidth"))
                builder.strokeWidth(Double.parseDouble(reader.getAttribute("strokeWidth")));
            if (reader.hasAttribute("dynamic"))
                builder.dynamic(Boolean.parseBoolean(reader.getAttribute("dynamic")));
            if (reader.hasAttribute("outline"))
                builder.outline(Boolean.parseBoolean(reader.getAttribute("outline")));
            if (reader.hasAttribute("dashes")) {
                double[] dashes = Stream.of(reader.getAttribute("dashes").split(",")).mapToDouble(Double::parseDouble).toArray();
                builder.dashes(dashes);
            }
            if (reader.hasAttribute("cap")) {
                switch (reader.getAttribute("cap")) {
                    case "butt":
                        builder.cap(StrokeLineCap.BUTT);
                        break;
                    case "square":
                        builder.cap(StrokeLineCap.SQUARE);
                        break;
                    case "round":
                    default:
                        builder.cap(StrokeLineCap.ROUND);
                        break;
                }
            }
            if (reader.hasAttribute("join")) {
                switch (reader.getAttribute("join")) {
                    case "bevel":
                        builder.join(StrokeLineJoin.BEVEL);
                        break;
                    case "round":
                        builder.join(StrokeLineJoin.ROUND);
                        break;
                    case "miter":
                    default:
                        builder.join(StrokeLineJoin.MITER);
                        break;
                }
            }
            DrawType drawType = builder.build();

            NodeList tagNodes = element.getElementsByTagName("tag");
            for (int j = 0; j < tagNodes.getLength(); j++) {
                Node tagNode = tagNodes.item(j);
                if (tagNode.hasAttributes()) {
                    reader = new XMLAttributeReader(tagNode.getAttributes());
                    if (reader.hasAttribute("key")) {
                        if (reader.hasAttribute("value")) {
                            drawTypeMap.put(reader.getAttribute("key"), reader.getAttribute("value"), drawType);
                        } else {
                            drawTypeMap.put(reader.getAttribute("key"), null, drawType);
                        }
                    }

                }
            }
        }

    }

    static class XMLAttributeReader {
        private final NamedNodeMap attributes;

        public XMLAttributeReader(NamedNodeMap attributes) {
            this.attributes = attributes;
        }

        private boolean hasAttribute(String item) {
            return attributes.getNamedItem(item) != null;
        }

        private String getAttribute(String item) {
            Attr attr = (Attr) attributes.getNamedItem(item);
            if (attr != null) return attr.getValue();
            else return "";
        }
    }

}
