/* Code for COMP261 Assignment
 */

package renderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
//import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

public class Renderer {

	private int imageWidth = 600;
	private int imageHeight = 600;

	// shape
	private String fileName;

	private List<Vector3D> lightsList = new ArrayList<Vector3D>();
	private List<Vector3D> currentLightsList = new ArrayList<Vector3D>();

	private float dirX;
	private float dirY;

	private List<Polygon> polygonList = new ArrayList<Polygon>();

	private float minX = Integer.MAX_VALUE;
	private float minY = Integer.MAX_VALUE;
	private float minZ = Integer.MAX_VALUE;
	private float maxX = Integer.MIN_VALUE;
	private float maxY = Integer.MIN_VALUE;
	private float maxZ = Integer.MIN_VALUE;

	private int margin = 20;

	private List<Float> ambientLevelList = new ArrayList<Float>();
	private float ambLvlShift = 0.05f;

	private List<Float> intensityLevelList = new ArrayList<Float>();
	private float intLvlShift = 0.05f;

	private JFrame frame;
	private BufferedImage image;
	private JComponent drawing;
	private JTextArea textOutput;

	private float scale;

	private String lastMove;

	private float shift = 0.1f;
	private float shiftX;
	private float shiftY;
	private float countDegrees;

	public static void main(String[] args) {
		new Renderer();
	}

	private Renderer() {
		setupFrame();
	}

	/**
	 * Creates a frame with a JComponent in it. Clicking in the frame will close
	 * it.
	 */
	private void setupFrame() {
		frame = new JFrame("Graphics Renderer");
		frame.setSize(imageWidth + 6, imageHeight + 116);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseReleased(e);
				frame.requestFocus();
			}
		});

		// set all frame components
		drawing = new JComponent() {
			protected void paintComponent(Graphics g) {
				g.drawImage(image, 0, 0, null);
				redraw();
			}
		};
		frame.add(drawing, BorderLayout.CENTER);

		JPanel panel = new JPanel();

		// add instruction label
		JLabel label = new JLabel(
				"Arrows: change view +/-: change ambient </>: change intensity Viewing Direction (degrees)",
				JLabel.CENTER);
		panel.add(label);

		label = new JLabel("Y-axis: ", JLabel.CENTER);
		panel.add(label);

		JTextField dirXInput = new JTextField(5);
		panel.add(dirXInput);
		dirXInput.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent fe) {
				JTextComponent comp = (JTextComponent) fe.getSource();
				try {
					dirX = (float) -Math.toRadians(Float.parseFloat(comp
							.getText()));
					for (int i = 0; i < currentLightsList.size(); i++) {
						currentLightsList.set(
								i,
								rotateDirXYAxis(lightsList.get(i), dirX
										+ shiftX, dirY + shiftY));
					}
					createImage(rotateShapeXYAxis(polygonList, shiftX + dirX,
							shiftY + dirY));
				} catch (NumberFormatException ex) {

				}
			}
		});

		label = new JLabel("X-axis: ", JLabel.CENTER);
		panel.add(label);

		JTextField dirYInput = new JTextField(5);
		panel.add(dirYInput);
		dirYInput.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent fe) {
				JTextComponent comp = (JTextComponent) fe.getSource();
				try {
					dirY = (float) -Math.toRadians(Float.parseFloat(comp
							.getText()));
					for (int i = 0; i < currentLightsList.size(); i++) {
						currentLightsList.set(
								i,
								rotateDirXYAxis(lightsList.get(i), dirX
										+ shiftX, dirY + shiftY));
					}
					createImage(rotateShapeXYAxis(polygonList, shiftX + dirX,
							shiftY + dirY));
				} catch (NumberFormatException ex) {
				}
			}
		});

		frame.add(panel, BorderLayout.NORTH);
		addButton("Render", panel, new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				// reset values
				resetValues();
				// load shape
				if (readFile()) {
					createImage(polygonList);
				}
			}
		});

		addButton("Add Light Source", panel, new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					Vector3D vector = toVector3D((String) JOptionPane
							.showInputDialog(
									frame,
									"Vector of Light Source (Separated by space)",
									"Vector of Light Source",
									JOptionPane.PLAIN_MESSAGE));
					float ambient = Float.parseFloat((String) JOptionPane
							.showInputDialog(frame, "Ambient of Light Source",
									"Ambient", JOptionPane.PLAIN_MESSAGE));
					float intensity = Float.parseFloat((String) JOptionPane
							.showInputDialog(frame,
									"Intensity of Light Source", "Intensity",
									JOptionPane.PLAIN_MESSAGE));

					// add light source to list
					lightsList.add(vector);
					currentLightsList.add(vector);

					// add ambient and intensity to list
					ambientLevelList.add(ambient);
					intensityLevelList.add(intensity);

					createImage(rotateShapeXYAxis(polygonList, shiftX + dirX,
							shiftY + dirY));

					JOptionPane.showMessageDialog(frame,
							"The light source has been added",
							"Input Successful", 1);

				} catch (NumberFormatException e) {
					JOptionPane.showMessageDialog(frame,
							"Cannot add the light source", "Input Error", 0);
				} catch (NullPointerException e) {
					JOptionPane.showMessageDialog(frame,
							"Cannot add the light source", "Input Error", 0);
				}
			}
		});

		addButton("Save", panel, new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				// save image
				saveImage("Image.png");
			}
		});
		addButton("Quit", panel, new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				System.exit(0);
			}
		});

		// make the arrow keys shift the image
		InputMap iMap = drawing.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap aMap = drawing.getActionMap();

		iMap.put(KeyStroke.getKeyStroke("LEFT"), "shiftLeft");
		iMap.put(KeyStroke.getKeyStroke("RIGHT"), "shiftRight");
		iMap.put(KeyStroke.getKeyStroke("UP"), "shiftUp");
		iMap.put(KeyStroke.getKeyStroke("DOWN"), "shiftDown");
		iMap.put(KeyStroke.getKeyStroke("PLUS"), "increaseAmbient");
		iMap.put(KeyStroke.getKeyStroke("ADD"), "increaseAmbient");
		iMap.put(KeyStroke.getKeyStroke("MINUS"), "decreaseAmbient");
		iMap.put(KeyStroke.getKeyStroke("SUBTRACT"), "decreaseAmbient");
		iMap.put(KeyStroke.getKeyStroke("PERIOD"), "increaseIntensity");
		iMap.put(KeyStroke.getKeyStroke("COMMA"), "decreaseIntensity");

		aMap.put("shiftLeft", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// rotate to left
				shiftX += (float) Math.round(shift * 100) / 100;
				for (int i = 0; i < currentLightsList.size(); i++) {
					currentLightsList.set(
							i,
							rotateDirXYAxis(lightsList.get(i), dirX + shiftX,
									dirY + shiftY));
				}
				createImage(rotateShapeXYAxis(polygonList, dirX + shiftX, dirY
						+ shiftY));
			}
		});
		aMap.put("shiftRight", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// rotate to right
				shiftX -= (float) Math.round(shift * 100) / 100;
				for (int i = 0; i < currentLightsList.size(); i++) {
					currentLightsList.set(
							i,
							rotateDirXYAxis(lightsList.get(i), dirX + shiftX,
									dirY + shiftY));
				}
				createImage(rotateShapeXYAxis(polygonList, dirX + shiftX, dirY
						+ shiftY));
			}
		});
		aMap.put("shiftUp", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// rotate to right
				shiftY -= (float) Math.round(shift * 100) / 100;
				for (int i = 0; i < currentLightsList.size(); i++) {
					currentLightsList.set(
							i,
							rotateDirXYAxis(lightsList.get(i), dirX + shiftX,
									dirY + shiftY));
				}
				createImage(rotateShapeXYAxis(polygonList, dirX + shiftX, dirY
						+ shiftY));
			}
		});
		aMap.put("shiftDown", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// rotate to right
				shiftY += (float) Math.round(shift * 100) / 100;
				for (int i = 0; i < currentLightsList.size(); i++) {
					currentLightsList.set(
							i,
							rotateDirXYAxis(lightsList.get(i), dirX + shiftX,
									dirY + shiftY));
				}
				createImage(rotateShapeXYAxis(polygonList, dirX + shiftX, dirY
						+ shiftY));
			}
		});
		aMap.put("increaseAmbient", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < ambientLevelList.size(); i++) {
					ambientLevelList.set(i,
							(float) ((double) Math.round((ambientLevelList
									.get(i) + ambLvlShift) * 100) / 100));
				}
				for (int i = 0; i < currentLightsList.size(); i++) {
					currentLightsList.set(
							i,
							rotateDirXYAxis(lightsList.get(i), dirX + shiftX,
									dirY + shiftY));
				}
				createImage(rotateShapeXYAxis(polygonList, dirX + shiftX, dirY
						+ shiftY));
			}
		});
		aMap.put("decreaseAmbient", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < ambientLevelList.size(); i++) {
					ambientLevelList.set(i,
							(float) ((double) Math.round((ambientLevelList
									.get(i) - ambLvlShift) * 100) / 100));
					if (ambientLevelList.get(i) < 0) {
						ambientLevelList.set(i, 0f);
					}
				}
				for (int i = 0; i < currentLightsList.size(); i++) {
					currentLightsList.set(
							i,
							rotateDirXYAxis(lightsList.get(i), dirX + shiftX,
									dirY + shiftY));
				}
				createImage(rotateShapeXYAxis(polygonList, dirX + shiftX, dirY
						+ shiftY));
			}
		});
		aMap.put("increaseIntensity", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < intensityLevelList.size(); i++) {
					intensityLevelList.set(i,
							(float) ((double) Math.round((intensityLevelList
									.get(i) + intLvlShift) * 100) / 100));
				}
				for (int i = 0; i < currentLightsList.size(); i++) {
					currentLightsList.set(
							i,
							rotateDirXYAxis(lightsList.get(i), dirX + shiftX,
									dirY + shiftY));
				}
				createImage(rotateShapeXYAxis(polygonList, dirX + shiftX, dirY
						+ shiftY));
			}
		});

		aMap.put("decreaseIntensity", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				for (int i = 0; i < intensityLevelList.size(); i++) {
					intensityLevelList.set(i,
							(float) ((double) Math.round((intensityLevelList
									.get(i) - intLvlShift) * 100) / 100));
					if (intensityLevelList.get(i) < 0) {
						intensityLevelList.set(i, 0f);
					}
				}

				for (int i = 0; i < currentLightsList.size(); i++) {
					currentLightsList.set(
							i,
							rotateDirXYAxis(lightsList.get(i), dirX + shiftX,
									dirY + shiftY));
				}

				createImage(rotateShapeXYAxis(polygonList, dirX + shiftX, dirY
						+ shiftY));
			}
		});

		textOutput = new JTextArea(3, 50);
		textOutput.setEditable(false);
		JScrollPane textSP = new JScrollPane(textOutput);
		frame.add(textSP, BorderLayout.SOUTH);
		frame.setVisible(true);
	}

	private Vector3D toVector3D(String s) {
		try {
			String[] tokens = s.split(" ");

			float x = Float.parseFloat(tokens[0]);
			float y = Float.parseFloat(tokens[1]);
			float z = Float.parseFloat(tokens[2]);

			return new Vector3D(x, y, z);
		} catch (NullPointerException e) {
			throw new NumberFormatException();
		} catch (NumberFormatException e) {
			throw new NumberFormatException();
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new NumberFormatException();
		}
	}

	private void redraw() {
		if (fileName != null) {
			String output = "Viewing " + new File(fileName).getName()
					+ " from (" + (double) Math.round(shiftX * 100) / 100 + ","
					+ (double) Math.round(shiftY * 100) / 100 + ")";
			for (int i = 0; i < currentLightsList.size(); i++) {
				Vector3D light = currentLightsList.get(i);
				output += "\nLight Source #" + (i + 1) + " at position ("
						+ light.x + ", " + light.y + ", " + light.z
						+ ") ambient: " + ambientLevelList.get(i)
						+ " intensity: " + intensityLevelList.get(i);
			}
			textOutput.setText(output);
		}
	}

	private void resetValues() {
		polygonList = new ArrayList<Polygon>();
		lightsList = new ArrayList<Vector3D>();
		dirX = 0;
		dirY = 0;
		currentLightsList = new ArrayList<Vector3D>();
		ambientLevelList = new ArrayList<Float>();
		intensityLevelList = new ArrayList<Float>();
		;
		shiftX = 0;
		shiftY = 0;
		resetBoundaries();
	}

	private void resetBoundaries() {
		minX = Integer.MAX_VALUE;
		minY = Integer.MAX_VALUE;
		maxX = Integer.MIN_VALUE;
		maxY = Integer.MIN_VALUE;
	}

	private void addButton(String name, JComponent comp, ActionListener listener) {
		JButton button = new JButton(name);
		comp.add(button);
		button.addActionListener(listener);
	}

	private String getDataDir() {
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if (fc.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		return fc.getSelectedFile().getPath() + File.separator;
	}

	class Polygon {
		private List<Vector3D> vectors;
		private Color reflectivity;
		private Color col;

		Polygon(List<Vector3D> vectors, Color reflectivity) {
			this.vectors = vectors;
			this.reflectivity = reflectivity;
		}

		Polygon(List<Vector3D> vectors, Color reflectivity, Color col) {
			this.vectors = vectors;
			this.reflectivity = reflectivity;
			this.col = col;
		}

	}

	// Helper Methods
	public List<Polygon> resizeShapeWithScale(List<Polygon> list, float sx,
			float sy, float sz) {
		// change all vector of light sources
		for (int i = 0; i < currentLightsList.size(); i++) {
			currentLightsList.set(
					i,
					Transform.newScale(sx, sy, sz).multiply(
							currentLightsList.get(i)));
		}
		List<Polygon> polygonListRendered = new ArrayList<Polygon>();
		// change all polygon vectors in original polygon list and add them into
		// rendered polygon list
		for (Polygon p : list) {
			Color pRef = new Color(p.reflectivity.getRed(),
					p.reflectivity.getGreen(), p.reflectivity.getBlue());
			Polygon pTemp = new Polygon(new ArrayList<Vector3D>(), pRef);
			polygonListRendered.add(pTemp);
			Color col = computeColor(p.vectors, p.reflectivity);
			p.col = col;
			Transform t = Transform.newScale(sx, sy, sz);
			for (int i = 0; i < p.vectors.size(); i++) {
				Vector3D vTemp = t.multiply(p.vectors.get(i));
				pTemp.vectors.add(vTemp);
			}
		}
		return polygonListRendered;
	}

	private Vector3D rotateDirXAxis(Vector3D dir, float th) {
		return Transform.newXRotation(th).multiply(dir);
	}

	private Vector3D rotateDirYAxis(Vector3D dir, float th) {
		return Transform.newYRotation(th).multiply(dir);
	}

	private Vector3D rotateDirXYAxis(Vector3D dir, float thX, float thY) {
		Transform t = Transform.newYRotation(thX);
		t = t.compose(Transform.newXRotation(thY));
		return t.multiply(dir);
	}

	private List<Polygon> translateShape(List<Polygon> list, float dx,
			float dy, float dz) {
		resetBoundaries();

		List<Polygon> polygonListRendered = new ArrayList<Polygon>();
		for (Polygon p : list) {
			Color pRef = new Color(p.reflectivity.getRed(),
					p.reflectivity.getGreen(), p.reflectivity.getBlue());
			Polygon pTemp = new Polygon(new ArrayList<Vector3D>(), pRef);
			polygonListRendered.add(pTemp);
			Transform trans = Transform.newTranslation(dx, dy, dz);
			for (int i = 0; i < p.vectors.size(); i++) {
				Vector3D vTemp = trans.multiply(p.vectors.get(i));
				pTemp.vectors.add(vTemp);
				if (vTemp.x < minX) {
					minX = Math.round(vTemp.x);
				}
				if (vTemp.y < minY) {
					minY = Math.round(vTemp.y);
				}
				if (vTemp.z < minZ) {
					minZ = Math.round(vTemp.z);
				}

				if (vTemp.x > maxX) {
					maxX = Math.round(vTemp.x);
				}
				if (vTemp.y > maxY) {
					maxY = Math.round(vTemp.y);
				}
				if (vTemp.z > maxZ) {
					maxZ = Math.round(vTemp.z);
				}
			}
		}
		if (maxX - minX > imageWidth || maxY - minY > imageHeight) {
			// resize polygons to fit the frame size
			float sx = (float) (imageWidth - 20) / (maxX - minX);
			float sy = (float) (imageHeight - 20) / (maxY - minY);

			scale = Math.min(sx, sy);

			polygonListRendered = resizeShapeWithScale(polygonListRendered,
					scale, scale, scale);
			resetBoundingBox(polygonListRendered);
		}
		return polygonListRendered;
	}

	private List<Polygon> rotateShapeXYAxis(List<Polygon> list, float thX,
			float thY) {
		resetBoundaries();

		List<Polygon> polygonListRendered = new ArrayList<Polygon>();
		for (Polygon p : list) {
			Color pRef = new Color(p.reflectivity.getRed(),
					p.reflectivity.getGreen(), p.reflectivity.getBlue());
			Polygon pTemp = new Polygon(new ArrayList<Vector3D>(), pRef);
			polygonListRendered.add(pTemp);

			Transform trans = Transform.newYRotation(thX);
			trans = trans.compose(Transform.newXRotation(thY));

			for (int i = 0; i < p.vectors.size(); i++) {
				Vector3D vTemp = trans.multiply(p.vectors.get(i));
				pTemp.vectors.add(vTemp);
				if (vTemp.x < minX) {
					minX = Math.round(vTemp.x);
				}
				if (vTemp.y < minY) {
					minY = Math.round(vTemp.y);
				}
				if (vTemp.z < minZ) {
					minZ = Math.round(vTemp.z);
				}

				if (vTemp.x > maxX) {
					maxX = Math.round(vTemp.x);
				}
				if (vTemp.y > maxY) {
					maxY = Math.round(vTemp.y);
				}
				if (vTemp.z > maxZ) {
					maxZ = Math.round(vTemp.z);
				}
			}
		}
		if (maxX - minX > imageWidth || maxY - minY > imageHeight) {
			// resize polygons to fit the frame size
			float sx = (float) (imageWidth - 20) / (maxX - minX);
			float sy = (float) (imageHeight - 20) / (maxY - minY);

			scale = Math.min(sx, sy);

			polygonListRendered = resizeShapeWithScale(polygonListRendered,
					scale, scale, scale);
			resetBoundingBox(polygonListRendered);
		}
		return polygonListRendered;
	}

	private List<Polygon> rotateShapeXAxis(List<Polygon> list, float th) {
		resetBoundaries();

		List<Polygon> polygonListRendered = new ArrayList<Polygon>();
		for (Polygon p : list) {
			Color pRef = new Color(p.reflectivity.getRed(),
					p.reflectivity.getGreen(), p.reflectivity.getBlue());
			Polygon pTemp = new Polygon(new ArrayList<Vector3D>(), pRef);
			polygonListRendered.add(pTemp);

			Transform trans = Transform.newXRotation(th);

			for (int i = 0; i < p.vectors.size(); i++) {
				Vector3D vTemp = trans.multiply(p.vectors.get(i));
				pTemp.vectors.add(vTemp);
				if (vTemp.x < minX) {
					minX = Math.round(vTemp.x);
				}
				if (vTemp.y < minY) {
					minY = Math.round(vTemp.y);
				}
				if (vTemp.z < minZ) {
					minZ = Math.round(vTemp.z);
				}

				if (vTemp.x > maxX) {
					maxX = Math.round(vTemp.x);
				}
				if (vTemp.y > maxY) {
					maxY = Math.round(vTemp.y);
				}
				if (vTemp.z > maxZ) {
					maxZ = Math.round(vTemp.z);
				}
			}
		}
		if (maxX - minX > imageWidth || maxY - minY > imageHeight) {
			// resize polygons to fit the frame size
			float sx = (float) (imageWidth - 20) / (maxX - minX);
			float sy = (float) (imageHeight - 20) / (maxY - minY);

			scale = Math.min(sx, sy);

			polygonListRendered = resizeShapeWithScale(polygonListRendered,
					scale, scale, scale);
			resetBoundingBox(polygonListRendered);
		}
		return polygonListRendered;
	}

	private List<Polygon> rotateShapeYAxis(List<Polygon> list, float th) {
		resetBoundaries();

		List<Polygon> polygonListRendered = new ArrayList<Polygon>();
		for (Polygon p : list) {
			Color pRef = new Color(p.reflectivity.getRed(),
					p.reflectivity.getGreen(), p.reflectivity.getBlue());
			Polygon pTemp = new Polygon(new ArrayList<Vector3D>(), pRef);
			polygonListRendered.add(pTemp);

			Transform trans = Transform.newYRotation(th);

			for (int i = 0; i < p.vectors.size(); i++) {
				Vector3D vTemp = trans.multiply(p.vectors.get(i));
				pTemp.vectors.add(vTemp);
				if (vTemp.x < minX) {
					minX = Math.round(vTemp.x);
				}
				if (vTemp.y < minY) {
					minY = Math.round(vTemp.y);
				}
				if (vTemp.z < minZ) {
					minZ = Math.round(vTemp.z);
				}

				if (vTemp.x > maxX) {
					maxX = Math.round(vTemp.x);
				}
				if (vTemp.y > maxY) {
					maxY = Math.round(vTemp.y);
				}
				if (vTemp.z > maxZ) {
					maxZ = Math.round(vTemp.z);
				}
			}
		}
		if (maxX - minX > imageWidth || maxY - minY > imageHeight) {
			// resize polygons to fit the frame size
			float sx = (float) (imageWidth - 20) / (maxX - minX);
			float sy = (float) (imageHeight - 20) / (maxY - minY);

			scale = Math.min(sx, sy);

			polygonListRendered = resizeShapeWithScale(polygonListRendered,
					scale, scale, scale);
			resetBoundingBox(polygonListRendered);
		}
		return polygonListRendered;
	}

	private List<Polygon> rotateShapeZAxis(List<Polygon> list, float th) {
		resetBoundaries();

		List<Polygon> polygonListRendered = new ArrayList<Polygon>();
		for (Polygon p : list) {
			Color pRef = new Color(p.reflectivity.getRed(),
					p.reflectivity.getGreen(), p.reflectivity.getBlue());
			Polygon pTemp = new Polygon(new ArrayList<Vector3D>(), pRef);
			polygonListRendered.add(pTemp);

			Transform trans = Transform.newZRotation(th);

			for (int i = 0; i < p.vectors.size(); i++) {
				Vector3D vTemp = trans.multiply(p.vectors.get(i));
				pTemp.vectors.add(vTemp);
				if (vTemp.x < minX) {
					minX = Math.round(vTemp.x);
				}
				if (vTemp.y < minY) {
					minY = Math.round(vTemp.y);
				}
				if (vTemp.z < minZ) {
					minZ = Math.round(vTemp.z);
				}

				if (vTemp.x > maxX) {
					maxX = Math.round(vTemp.x);
				}
				if (vTemp.y > maxY) {
					maxY = Math.round(vTemp.y);
				}
				if (vTemp.z > maxZ) {
					maxZ = Math.round(vTemp.z);
				}
			}
		}
		if (maxX - minX > imageWidth || maxY - minY > imageHeight) {
			// resize polygons to fit the frame size
			float sx = (float) (imageWidth - 20) / (maxX - minX);
			float sy = (float) (imageHeight - 20) / (maxY - minY);

			scale = Math.min(sx, sy);

			polygonListRendered = resizeShapeWithScale(polygonListRendered,
					scale, scale, scale);
			resetBoundingBox(polygonListRendered);
		}
		return polygonListRendered;
	}

	// compare vectors based on their axis
	// order by the smallest axis value
	class VectorYComparator implements Comparator<Vector3D> {
		@Override
		public int compare(Vector3D v1, Vector3D v2) {
			return (v1.y < v2.y) ? -1 : 1;
		}
	}

	class VectorXComparator implements Comparator<Vector3D> {
		@Override
		public int compare(Vector3D v1, Vector3D v2) {
			return (v1.x < v2.x) ? -1 : 1;
		}
	}

	class VectorZComparator implements Comparator<Vector3D> {
		@Override
		public int compare(Vector3D v1, Vector3D v2) {
			return (v1.z < v2.z) ? -1 : 1;
		}
	}

	/**
	 * Converts a 2D array of Colors to a BufferedImage. Assumes that bitmap is
	 * indexed by column then row and has imageHeight rows and imageWidth
	 * columns. Note that image.setRGB requires x (col) and y (row) are given in
	 * that order.
	 */
	private BufferedImage convertBitmapToImage(Color[][] bitmap) {
		image = new BufferedImage(bitmap.length, bitmap[0].length,
				BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < bitmap.length; x++) {
			for (int y = 0; y < bitmap[0].length; y++) {
				image.setRGB(x, y, bitmap[x][y].getRGB());
			}
		}
		return image;
	}

	private void resetBoundingBox(List<Polygon> list) {
		resetBoundaries();

		for (Polygon p : list) {
			for (Vector3D temp : p.vectors) {
				if (temp.x < minX) {
					minX = Math.round(temp.x);
				}
				if (temp.y < minY) {
					minY = Math.round(temp.y);
				}
				if (temp.z < minZ) {
					minZ = Math.round(temp.z);
				}

				if (temp.x > maxX) {
					maxX = Math.round(temp.x);
				}
				if (temp.y > maxY) {
					maxY = Math.round(temp.y);
				}
				if (temp.z > maxZ) {
					maxZ = Math.round(temp.z);
				}
			}
		}
	}

	private boolean readFile() {
		fileName = getDataDir();
		if (fileName != null) {
			BufferedReader data;
			try {
				data = new BufferedReader(new FileReader(fileName));
				// get the direction
				String[] floatDir = data.readLine().split(" ");
				// exit if data is not complete
				if (floatDir.length != 3) {
					textOutput.append("Cannot read the file ");
					data.close();
					return false;
				}
				Vector3D light = new Vector3D(Float.parseFloat(floatDir[0]),
						Float.parseFloat(floatDir[1]),
						Float.parseFloat(floatDir[2]));

				Vector3D currentLight = light;

				lightsList.add(light);
				currentLightsList.add(currentLight);

				// add default ambient and intensity
				ambientLevelList.add(0.5f);

				intensityLevelList.add(1f);

				String line = data.readLine();
				while (line != null) {
					String[] tokens = line.split(" ");
					List<Vector3D> vectors = new ArrayList<Vector3D>();
					for (int i = 0; i < tokens.length - 3; i += 3) {
						float x = Float.parseFloat(tokens[i]);
						float y = Float.parseFloat(tokens[i + 1]);
						float z = Float.parseFloat(tokens[i + 2]);

						Vector3D temp = new Vector3D(x, y, z);

						vectors.add(temp);

						if (temp.x < minX) {
							minX = Math.round(temp.x);
						}
						if (temp.y < minY) {
							minY = Math.round(temp.y);
						}
						if (temp.z < minZ) {
							minZ = Math.round(temp.z);
						}

						if (temp.x > maxX) {
							maxX = Math.round(temp.x);
						}
						if (temp.y > maxY) {
							maxY = Math.round(temp.y);
						}
						if (temp.z > maxZ) {
							maxZ = Math.round(temp.z);
						}

					}

					int red = Integer.parseInt(tokens[tokens.length - 3]);
					int green = Integer.parseInt(tokens[tokens.length - 2]);
					int blue = Integer.parseInt(tokens[tokens.length - 1]);

					if (red > 255)
						red = 255;
					if (red < 0)
						red = 0;

					if (green > 255)
						green = 255;
					if (green < 0)
						green = 0;

					if (blue > 255)
						blue = 255;
					if (blue < 0)
						blue = 0;

					Color ref = new Color(red, green, blue);

					// set color
					Color col = computeColor(vectors, ref);

					// add to list
					polygonList.add(new Polygon(vectors, ref, col));
					line = data.readLine();
				}
				data.close();

				// translate shape to the origin (if needed)
				float dx = 0;
				float dy = 0;
				float dz = 0;
				if (minX < 0 && maxX > 0) {
					dx = -minX;
				}
				if (minY < 0 && maxY > 0) {
					dy = -minY;
				}
				if (minZ < 0 && maxZ > 0) {
					dz = -minZ;
				}
				if (dx != 0 || dy != 0 || dz != 0) {
					polygonList = translateShape(polygonList, dx, dy, dz);
				}

				if (maxX - minX > imageWidth || maxY - minY > imageHeight) {
					// resize polygons to fit the frame size
					float sx = (float) (imageWidth - 20) / (maxX - minX);
					float sy = (float) (imageHeight - 20) / (maxY - minY);

					scale = Math.min(sx, sy);
					polygonList = resizeShapeWithScale(polygonList, scale,
							scale, scale);
					resetBoundingBox(polygonList);
				} else {
					scale = 1;
				}
			} catch (IOException e) {
				textOutput.append("Failed to open the file ");
			} catch (NumberFormatException e) {
				textOutput.append("Cannot read the file ");
			}
		} else {
			return false;
		}
		return true;
	}

	private Color computeColor(List<Vector3D> vectors, Color ref) {

		// remove if (x2-x1)*(y3-y2) > (y2-y1)*(x3-x2)
		if ((vectors.get(1).x - vectors.get(0).x)
				* (vectors.get(2).y - vectors.get(1).y) > (vectors.get(1).y - vectors
				.get(0).y) * (vectors.get(2).x - vectors.get(1).x)) {
			return Color.gray;
		}

		List<Color> colorsList = new ArrayList<Color>();

		for (Vector3D currentLight : currentLightsList) {
			Vector3D normal = (vectors.get(1).minus(vectors.get(0)))
					.crossProduct((vectors.get(2).minus(vectors.get(1))));
			Vector3D unitNormal = normal.unitVector();
			Vector3D unitDir = currentLight.unitVector();
			float costh = unitNormal.dotProduct(unitDir);

			if (costh > 1)
				costh = 1;
			else if (costh < 0)
				costh = 0;

			int index = currentLightsList.indexOf(currentLight);

			float newRed = ref.getRed()
					* (ambientLevelList.get(index) + intensityLevelList
							.get(index) * costh) / 255;
			float newGreen = ref.getGreen()
					* (ambientLevelList.get(index) + intensityLevelList
							.get(index) * costh) / 255;
			float newBlue = ref.getBlue()
					* (ambientLevelList.get(index) + intensityLevelList
							.get(index) * costh) / 255;

			if (newRed < 0)
				newRed = 0;
			if (newRed > 1)
				newRed = 1;

			if (newGreen < 0)
				newGreen = 0;
			if (newGreen > 1)
				newGreen = 1;

			if (newBlue < 0)
				newBlue = 0;
			if (newBlue > 1)
				newBlue = 1;

			colorsList.add(new Color(newRed, newGreen, newBlue));
		}

		float newRed = 0;
		float newGreen = 0;
		float newBlue = 0;

		for (Color c : colorsList) {
			newRed += (float) c.getRed() / 255;
			newGreen += (float) c.getGreen() / 255;
			newBlue += (float) c.getBlue() / 255;
		}

		if (newRed < 0)
			newRed = 0;
		if (newRed > 1)
			newRed = 1;

		if (newGreen < 0)
			newGreen = 0;
		if (newGreen > 1)
			newGreen = 1;

		if (newBlue < 0)
			newBlue = 0;
		if (newBlue > 1)
			newBlue = 1;

		return new Color(newRed, newGreen, newBlue);

	}

	private float[][] computeEdgeList(List<Vector3D> vectors) {

		int minimumY = Math.round(minY);
		int maximumY = Math.round(maxY);

		float[][] edgeList = new float[maximumY - minimumY + 1 + 2 * margin][];

		// initialise array with illegal values
		for (int j = 0; j < edgeList.length; j++) {
			edgeList[j] = new float[] { Float.POSITIVE_INFINITY,
					Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
					Float.POSITIVE_INFINITY };
		}

		List<Vector3D> vectorsTemp = new ArrayList<Vector3D>();

		for (Vector3D v : vectors) {
			vectorsTemp.add(v);
		}

		// sort the vectors based on y axis
		Collections.sort(vectorsTemp, new VectorYComparator());

		for (int count = 1; count < vectorsTemp.size(); count++) {
			Vector3D vTemp = vectorsTemp.get(0); // vector with smallest y value
			float mx = (float) (vectorsTemp.get(count).x - vTemp.x)
					/ (vectorsTemp.get(count).y - vTemp.y);
			float mz = (float) (vectorsTemp.get(count).z - vTemp.z)
					/ (vectorsTemp.get(count).y - vTemp.y);

			float x = vTemp.x - minX + margin;
			float z = vTemp.z;
			int i = Math.round(vTemp.y) - minimumY + margin;
			int maxi = Math.round(vectorsTemp.get(count).y) - minimumY + margin;

			do {
				if (x < edgeList[i][0]) {
					edgeList[i][0] = x;
					edgeList[i][1] = z;
				}
				if (x > edgeList[i][2]) {
					edgeList[i][2] = x;
					edgeList[i][3] = z;
				}
				i++;
				x = x + mx;
				z = z + mz;
			} while (i < maxi);

			edgeList[maxi] = new float[] {
					vectorsTemp.get(count).x - minX + margin,
					vectorsTemp.get(count).z,
					vectorsTemp.get(count).x - minX + margin,
					vectorsTemp.get(count).z };

			if (vTemp == vectorsTemp.get(0) && count == 2) {
				vTemp = vectorsTemp.get(1); // vector with second-smallest y
											// value
				mx = (float) (vectorsTemp.get(count).x - vTemp.x)
						/ (vectorsTemp.get(count).y - vTemp.y);
				mz = (float) (vectorsTemp.get(count).z - vTemp.z)
						/ (vectorsTemp.get(count).y - vTemp.y);

				x = vTemp.x - minX + margin;
				z = vTemp.z;
				i = Math.round(vTemp.y) - minimumY + margin;
				maxi = Math.round(vectorsTemp.get(count).y) - minimumY + margin;

				do {
					if (x < edgeList[i][0]) {
						edgeList[i][0] = x;
						edgeList[i][1] = z;
					}
					if (x > edgeList[i][2]) {
						edgeList[i][2] = x;
						edgeList[i][3] = z;
					}
					i++;
					x = x + mx;
					z = z + mz;
				} while (i < maxi);
				edgeList[maxi] = new float[] {
						vectorsTemp.get(count).x - minX + margin,
						vectorsTemp.get(count).z,
						vectorsTemp.get(count).x - minX + margin,
						vectorsTemp.get(count).z };
			}
		}
		return edgeList;
	}

	private void createImage(List<Polygon> list) {
		if (list == null || list.isEmpty())
			return;
		int maximumX = Math.round(maxX);
		int minimumX = Math.round(minX);
		int maximumY = Math.round(maxY);
		int minimumY = Math.round(minY);
		Color[][] imageColors = new Color[maximumX - minimumX + 1 + 2 * margin][maximumY
				- minimumY + 1 + 2 * margin];
		float[][] zValues = new float[maximumX - minimumX + 1 + 2 * margin][maximumY
				- minimumY + 1 + 2 * margin];

		// initialize all values
		for (int j = 0; j < imageColors[0].length; j++) {
			for (int i = 0; i < imageColors.length; i++) {
				imageColors[i][j] = Color.gray;
				zValues[i][j] = Float.POSITIVE_INFINITY;
			}
		}

		for (Polygon vr : list) {
			// compute edge list
			float[][] edgeList = computeEdgeList(vr.vectors);
			// compute color
			vr.col = computeColor(vr.vectors, vr.reflectivity);
			// compute shading
			for (int y = 0; y < edgeList.length; y++) {
				float mz = (float) (edgeList[y][3] - edgeList[y][1])
						/ (edgeList[y][2] - edgeList[y][0]);
				int x = Math.round(edgeList[y][0]);
				float z = edgeList[y][1];

				while (x >= 0 && x < imageColors.length
						&& x <= Math.round(edgeList[y][2])) {
					if (z < zValues[x][y]) {
						zValues[x][y] = z;
						imageColors[x][y] = new Color(vr.col.getRed(),
								vr.col.getGreen(), vr.col.getBlue());

					}
					x++;
					z = z + mz;
				}
			}

		}

		// copy array of shape image to array of canvas
		Color[][] bitmap = new Color[imageWidth][imageHeight];

		int startX = (imageWidth - imageColors.length) / 2;
		int endX = startX + imageColors.length - 1;
		int startY = (imageHeight - imageColors[0].length) / 2;
		int endY = startY + imageColors[0].length - 1;

		for (int i = 0; i < bitmap.length; i++) {
			for (int j = 0; j < bitmap[i].length; j++) {
				if (i >= startX && i <= endX && j >= startY && j <= endY) {
					bitmap[i][j] = imageColors[i - startX][j - startY];
				} else {
					bitmap[i][j] = Color.gray;
				}
			}
		}

		// render the bitmap to the image so it can be displayed (and saved)
		image = convertBitmapToImage(bitmap);
		// draw it.
		drawing.repaint();
	}

	/**
	 * writes the current image to a file of the specified name
	 */
	private void saveImage(String fname) {
		try {
			ImageIO.write(image, "png", new File(fname));
		} catch (IOException e) {
			System.out.println("Image saving failed: " + e);
		} catch (NullPointerException e) {
			System.out.println("No image will be saved.");
		}
	}

}
