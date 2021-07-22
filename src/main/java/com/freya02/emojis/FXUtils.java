package com.freya02.emojis;

import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.transform.Transform;
import org.slf4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class FXUtils {
	private static final Logger LOGGER = Logging.getLogger();
	private static final Pattern TRANSFORM_PATTERN = Pattern.compile("(.+?)\\((.*)\\)");

	private static final SAXParserFactory factory;

	static {
		try {
			factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** This method looks like it doesn't need the FX thread */
	public static List<Shape> getSvgs(InputStream resource) {
		try {
			List<Shape> svgList = new ArrayList<>();

			factory.newSAXParser().parse(resource, new DefaultHandler() {
				private FillRule activeFillRule = null;
				private Color activeColor = null;

				private double getDouble(Attributes attributes, String r) {
					return Double.parseDouble(attributes.getValue(r));
				}

				@Override
				public void startElement(String uri, String localName, String qName, Attributes attributes) {
					Shape shape = null;

					switch (qName) {
						case "svg":
							return;
						case "g": {
							final int length = attributes.getLength();

							for (int i = 0; i < length; i++) {
								final String name = attributes.getQName(i);
								switch (name) {
									case "fill":
										activeColor = Color.web(attributes.getValue(i));
										break;
									case "fill-rule":
										activeFillRule = attributes.getValue(i).equals("evenodd") ? FillRule.EVEN_ODD : FillRule.NON_ZERO;
										break;
									case "clip-rule":
										//no-op, no JFX counterpart
										break;
									default:
										LOGGER.warn("Unknown group (<g>) property : '{}'", name);
										break;
								}
							}

							return;
						}
						case "path":
							final SVGPath svgPath = new SVGPath();
							if (attributes.getValue("d") != null) svgPath.setContent(attributes.getValue("d"));
							if (activeFillRule != null) svgPath.setFillRule(activeFillRule);
							if (attributes.getValue("fill-rule") != null) svgPath.setFillRule(attributes.getValue("fill-rule").equals("evenodd") ? FillRule.EVEN_ODD : FillRule.NON_ZERO);

							shape = svgPath;
							break;
						case "circle":
							final Circle circle = new Circle();
							if (attributes.getValue("cx") != null) circle.setCenterX(getDouble(attributes, "cx"));
							if (attributes.getValue("cy") != null) circle.setCenterY(getDouble(attributes, "cy"));
							if (attributes.getValue("r") != null) circle.setRadius(getDouble(attributes, "r"));

							shape = circle;

							break;
						case "ellipse":
							final Ellipse ellipse = new Ellipse();

							if (attributes.getValue("cx") != null) ellipse.setCenterX(getDouble(attributes, "cx"));
							if (attributes.getValue("cy") != null) ellipse.setCenterY(getDouble(attributes, "cy"));
							if (attributes.getValue("rx") != null) ellipse.setRadiusX(getDouble(attributes, "rx"));
							if (attributes.getValue("ry") != null) ellipse.setRadiusY(getDouble(attributes, "ry"));

							shape = ellipse;

							break;
					}

					if (shape != null) {
						if (activeColor != null) {
							shape.setFill(activeColor);
						} else {
							final String fill = attributes.getValue("fill");
							if (fill != null) {
								if (fill.equals("none")) {
									shape.setFill(Color.TRANSPARENT);
								} else {
									shape.setFill(Color.web(fill));
								}
							}
						}

						//Final transforms
						final String transforms = attributes.getValue("transform");
						if (transforms != null) {
							final Matcher matcher = TRANSFORM_PATTERN.matcher(transforms);
							if (matcher.find()) {
								final String transformType = matcher.group(1);
								final String[] numbers = matcher.group(2).split(" ");
								if ("rotate".equals(transformType)) {
									if (numbers.length == 3) {
										shape.getTransforms().add(
												Transform.rotate(
														Double.parseDouble(numbers[0]),
														Double.parseDouble(numbers[1]),
														Double.parseDouble(numbers[2])
												));
									} else {
										LOGGER.warn("Invalid transform : {}, data : {}", transformType, matcher.group(2));
									}
								} else if ("matrix".equals(transformType)) {
									if (numbers.length == 6) {
										shape.getTransforms().add(
												Transform.affine(
														Double.parseDouble(numbers[0]),
														Double.parseDouble(numbers[1]),
														Double.parseDouble(numbers[2]),
														Double.parseDouble(numbers[3]),
														Double.parseDouble(numbers[4]),
														Double.parseDouble(numbers[5])
												));
									} else {
										LOGGER.warn("Invalid transform : {}, data : {}", transformType, matcher.group(2));
									}
								} else {
									LOGGER.warn("Unknown transform : {}", transformType);
								}
							}
						}

						svgList.add(shape);
					}
				}

				@Override
				public void endElement(String uri, String localName, String qName) {
					if (qName.equals("g")) {
						activeColor = null;
						activeFillRule = null;
					}
				}
			});

			return svgList;
		} catch (IOException | SAXException | ParserConfigurationException e) {
			throw new RuntimeException("Error reading SVG file", e);
		}
	}
}