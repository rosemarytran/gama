package msi.gama.jogl.scene;

import static javax.media.opengl.GL.*;
import java.awt.Font;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import msi.gama.jogl.utils.JOGLAWTGLRenderer;
import msi.gama.jogl.utils.JTSGeometryOpenGLDrawer.JTSDrawer;
import msi.gama.runtime.exceptions.GamaRuntimeException;

import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import com.sun.opengl.util.GLUT;
import com.sun.opengl.util.j2d.TextRenderer;
import com.sun.opengl.util.texture.*;
import com.vividsolutions.jts.geom.*;

public abstract class ObjectDrawer<T extends AbstractObject> {

	final JOGLAWTGLRenderer renderer;
	final GLUT glut = new GLUT();

	// final GL gl;

	public ObjectDrawer(JOGLAWTGLRenderer r) {
		renderer = r;
		// gl = r.gl;
	}

	// Better to subclass _draw than this one
	void draw(T object) {
		renderer.gl.glPushMatrix();
		renderer.gl.glTranslated(object.offset.x, -object.offset.y, object.offset.z);
		renderer.gl.glScaled(object.scale.x, object.scale.y, 1);
		_draw(object);
		renderer.gl.glPopMatrix();
	}

	protected abstract void _draw(T object);

	/**
	 * 
	 * The class ImageDrawer.
	 * 
	 * @author drogoul
	 * @since 4 mai 2013
	 * 
	 */
	public static class ImageDrawer extends ObjectDrawer<ImageObject> {

		public float textureTop, textureBottom, textureLeft, textureRight;

		public ImageDrawer(JOGLAWTGLRenderer r) {
			super(r);
		}

		@Override
		protected void _draw(ImageObject img) {
			MyTexture curTexture = renderer.getScene().getTextures().get(img.image);
			if ( curTexture == null ) { return; }
			// Enable the texture
			renderer.gl.glEnable(GL_TEXTURE_2D);
			Texture t = curTexture.texture;
			t.enable();
			t.bind();
			// Reset opengl color. Set the transparency of the image to
			// 1 (opaque).
			renderer.gl.glColor4d(1.0d, 1.0d, 1.0d, img.alpha);
			TextureCoords textureCoords;
			textureCoords = t.getImageTexCoords();
			textureTop = textureCoords.top();
			textureBottom = textureCoords.bottom();
			textureLeft = textureCoords.left();
			textureRight = textureCoords.right();
			if ( img.angle != 0 ) {
				renderer.gl.glTranslated(img.x + img.width / 2, -(img.y + img.height / 2), 0.0d);
				// FIXME:Check counterwise or not, and do we rotate
				// around the center or around a point.
				renderer.gl.glRotated(-img.angle, 0.0d, 0.0d, 1.0d);
				renderer.gl.glTranslated(-(img.x + img.width / 2), +(img.y + img.height / 2), 0.0d);

				renderer.gl.glBegin(GL_QUADS);
				// bottom-left of the texture and quad
				renderer.gl.glTexCoord2f(textureLeft, textureBottom);
				renderer.gl.glVertex3d(img.x, -(img.y + img.height), img.z);
				// bottom-right of the texture and quad
				renderer.gl.glTexCoord2f(textureRight, textureBottom);
				renderer.gl.glVertex3d(img.x + img.width, -(img.y + img.height), img.z);
				// top-right of the texture and quad
				renderer.gl.glTexCoord2f(textureRight, textureTop);
				renderer.gl.glVertex3d(img.x + img.width, -img.y, img.z);
				// top-left of the texture and quad
				renderer.gl.glTexCoord2f(textureLeft, textureTop);
				renderer.gl.glVertex3d(img.x, -img.y, img.z);
				renderer.gl.glEnd();
				renderer.gl.glTranslated(img.x + img.width / 2, -(img.y + img.height / 2), 0.0d);
				renderer.gl.glRotated(img.angle, 0.0d, 0.0d, 1.0d);
				renderer.gl.glTranslated(-(img.x + img.width / 2), +(img.y + img.height / 2), 0.0d);
			} else {
				renderer.gl.glBegin(GL_QUADS);
				// bottom-left of the texture and quad
				renderer.gl.glTexCoord2f(textureLeft, textureBottom);
				renderer.gl.glVertex3d(img.x, -(img.y + img.height), img.z);
				// bottom-right of the texture and quad
				renderer.gl.glTexCoord2f(textureRight, textureBottom);
				renderer.gl.glVertex3d(img.x + img.width, -(img.y + img.height), img.z);
				// top-right of the texture and quad
				renderer.gl.glTexCoord2f(textureRight, textureTop);
				renderer.gl.glVertex3d(img.x + img.width, -img.y, img.z);
				// top-left of the texture and quad
				renderer.gl.glTexCoord2f(textureLeft, textureTop);
				renderer.gl.glVertex3d(img.x, -img.y, img.z);
				renderer.gl.glEnd();
			}
			renderer.gl.glDisable(GL_TEXTURE_2D);
		}
	}
	
	/**
	 * 
	 * The class DEMDrawer.
	 * 
	 * @author grignard
	 * @since 15 mai 2013
	 * 
	 */
	public static class DEMDrawer extends ObjectDrawer<DEMObject> {

		private boolean initialized;

		public DEMDrawer(JOGLAWTGLRenderer r) {
			super(r);
		}
		
		public Texture loadTexture(String fileName) {
			Texture text = null;
			try {
				if ( renderer.getContext() != null ) {
					renderer.getContext().makeCurrent();
					text = TextureIO.newTexture(new File(fileName), false);
					text.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
					text.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
				} else {
					throw GamaRuntimeException.error("(DEM) JOGLRenderer context is null");
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
				System.out.println("Error loading texture " + fileName);
			}
			return text;
		}
		
		
		private BufferedImage FlipUpSideDownImage(BufferedImage img){
				java.awt.geom.AffineTransform tx = java.awt.geom.AffineTransform.getScaleInstance(1, -1);
				tx.translate(0, -img.getHeight(null));
				AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
				img = op.filter(img, null);
				return img;
			
		}
		
		private BufferedImage FlipRightSideLeftImage(BufferedImage img){
			java.awt.geom.AffineTransform tx = java.awt.geom.AffineTransform.getScaleInstance(-1, 1);
			tx.translate(-img.getWidth(null), 0);
			AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
			img = op.filter(img, null);
			return img;
		
	    }

	    @Override
		protected void _draw(DEMObject demObj) {
			
			if ( !isInitialized() ) {
				renderer.gl.glEnable(GL.GL_TEXTURE_2D);
			    loadTexture(demObj.texture.toString());
				setInitialized(true);
			}

			
			int rows, cols;
			int x, y;
			float vx, vy, s, t;
			float ts, tt, tw, th;

			//BufferedImage dem = readPNGImage(demFileName);
			BufferedImage dem = demObj.dem;
			dem = FlipUpSideDownImage(dem);
			dem = FlipRightSideLeftImage(dem);
			
			rows = dem.getHeight() - 1;
			cols = dem.getWidth() - 1;
			ts = 1.0f / cols;
			tt = 1.0f / rows;

			//FIXME/ need to set w and h dynamicly
			float w = (float) demObj.envelope.getWidth();
			float h = (float) demObj.envelope.getHeight();
			
			float altFactor = (float)demObj.envelope.getWidth()/(20*255);//0.025f;//dem.getWidth();
			
			tw = w / cols;
			th = h / rows;

			renderer.gl.glTranslated(w/2, -h/2, 0);
			
			renderer.gl.glNormal3f(0.0f, 1.0f, 0.0f);

			for ( y = 0; y < rows; y++ ) {
				renderer.gl.glBegin(GL.GL_QUAD_STRIP);
				for ( x = 0; x <= cols; x++ ) {
					vx = tw * x - w / 2.0f;
					vy = th * y - h / 2.0f;
					s = 1.0f - ts * x;
					t = 1.0f - tt * y;

					float alt1 = (dem.getRGB(cols - x, y) & 255) * altFactor;
					float alt2 = (dem.getRGB(cols - x, y + 1) & 255) * altFactor;
					
					boolean isTextured = true;
					if(isTextured){
						renderer.gl.glTexCoord2f(s, t);
						renderer.gl.glVertex3f(vx, vy, alt1);
						renderer.gl.glTexCoord2f(s, t - tt);
						renderer.gl.glVertex3f(vx, vy + th, alt2);	
					}
					else{
						float color = ((dem.getRGB(cols - x, y) & 255));
						color = (color)/255.0f;
						/*System.out.println("dem:" + dem.getRGB(cols - x, y));
						System.out.println("dem&:" + (dem.getRGB(cols - x, y) & 255));
						System.out.println("color" + color);*/

						renderer.gl.glColor3f(color, color, color);
						renderer.gl.glVertex3f(vx, vy, alt1);
						renderer.gl.glVertex3f(vx, vy + th, alt2);	
					}
					
				}
				renderer.gl.glEnd();
			}
			renderer.gl.glTranslated(-w/2, h/2, 0);
			
			//FIXME: Add disable texture?

		}
		
		
		//@Override
		protected void _drawOLd(DEMObject demObj) {
		
		    if ( !isInitialized() ) {
				//renderer.gl.glEnable(GL.GL_TEXTURE_2D);
			    //loadTexture(demObj.texture.toString());
				setInitialized(true);
			}
	
			int rows, cols;
			int x, y;
			float vx, vy, s, t;
			float ts, tt, tw, th;


			BufferedImage dem = demObj.dem;
		
			rows = dem.getHeight()-1;
			cols = dem.getWidth();
			//System.out.println("rows: " + rows + " cols" +cols );
			//ts = 1.0f / cols;
			//tt = 1.0f / rows;

			//FIXME/ need to set w and h dynamicly
			//float w = (float) demObj.envelope.getWidth();
			//float h = (float) demObj.envelope.getHeight();
			
			float altFactor = (float)demObj.envelope.getWidth()/(20*255);//0.025f;//dem.getWidth();
			
			//tw = w / cols;
			//th = h / rows;

			for ( y = 0; y < rows; y++ ) {
				renderer.gl.glBegin(GL.GL_QUAD_STRIP);
				for ( x = 0; x < cols; x++ ) {

					float alt1 = (dem.getRGB(x, y) & 255) * altFactor;
					float alt2 = (dem.getRGB(x, y + 1) & 255) * altFactor;
					
					
					boolean isTextured = false;

						int color = ((dem.getRGB(x, y) & 255));
						 int alpha = (color >> 24) & 0xff;
						    int red = (color >> 16) & 0xff;
						    int green = (color >> 8) & 0xff;
						    int blue = (color) & 0xff;
						    System.out.println("argb: " + alpha + ", " + red + ", " + green + ", " + blue);
					
						//color = (float)(Math.random() *255.0f)/255.0f ;
						//alt1 = (float)(Math.random() *20.0f);
						//alt2 = (float)(Math.random() *20.0f);
						//System.out.println("x: " + x + " y:" +y + " alt1:" + alt1 + " color:" + color);
						renderer.gl.glColor3f(red, green, blue);
						renderer.gl.glVertex3f(x, -y, alt1);
						renderer.gl.glVertex3f(x, -(y + 1), alt2);	
					}
				renderer.gl.glEnd();
				}
				
			//renderer.gl.glTranslated(-w/2, h/2, 0);
			}
			
			
			public boolean isInitialized() {
				
				return initialized;
			}



			public void setInitialized(boolean initialized) {
				this.initialized = initialized;
			}
	}

	/**
	 * 
	 * The class GeometryDrawer.
	 * 
	 * @author drogoul
	 * @since 4 mai 2013
	 * 
	 */
	public static class GeometryDrawer extends ObjectDrawer<GeometryObject> {

		JTSDrawer jtsDrawer;

		public GeometryDrawer(JOGLAWTGLRenderer r) {
			super(r);
			jtsDrawer = new JTSDrawer(r);
		}

		@Override
		protected void _draw(GeometryObject geometry) {
			// Rotate angle (in XY plan)
			if ( geometry.angle != 0 ) {
				renderer.gl.glTranslated(geometry.geometry.getCentroid().getX(), this.jtsDrawer.yFlag *
					geometry.geometry.getCentroid().getY(), 0.0d);
				renderer.gl.glRotated(-geometry.angle, 0.0d, 0.0d, 1.0d);
				renderer.gl.glTranslated(-geometry.geometry.getCentroid().getX(), -this.jtsDrawer.yFlag *
					geometry.geometry.getCentroid().getY(), 0.0d);
			}
			for ( int i = 0; i < geometry.geometry.getNumGeometries(); i++ ) {
				if ( geometry.geometry.getGeometryType() == "MultiPolygon" ) {
					jtsDrawer.DrawMultiPolygon((MultiPolygon) geometry.geometry, geometry.z_layer, geometry.color,
						geometry.alpha, geometry.fill, geometry.border, geometry.angle, geometry.height,
						geometry.rounded);
				} else if ( geometry.geometry.getGeometryType() == "Polygon" ) {
					// The JTS geometry of a sphere is a circle (a polygon)
					if ( geometry.type.equals("sphere") ) {
						jtsDrawer.DrawSphere(geometry.agent.getLocation(), geometry.z_layer, geometry.height,
							geometry.color, geometry.alpha);
					} else {
						if ( geometry.height > 0 ) {
							jtsDrawer.DrawPolyhedre((Polygon) geometry.geometry, geometry.z_layer, geometry.color,
								geometry.alpha, geometry.fill, geometry.height, geometry.angle, true, geometry.border,
								geometry.rounded);
						} else {
							jtsDrawer.DrawPolygon((Polygon) geometry.geometry, geometry.z_layer, geometry.color,
								geometry.alpha, geometry.fill, geometry.border, geometry.isTextured, geometry.angle,
								true, geometry.rounded);
						}
					}
				} else if ( geometry.geometry.getGeometryType() == "MultiLineString" ) {

					jtsDrawer.DrawMultiLineString((MultiLineString) geometry.geometry, geometry.z_layer,
						geometry.color, geometry.alpha, geometry.height);
				} else if ( geometry.geometry.getGeometryType() == "LineString" ) {

					if ( geometry.height > 0 ) {
						jtsDrawer.DrawPlan((LineString) geometry.geometry, geometry.z_layer, geometry.color,
							geometry.alpha, geometry.height, 0, true);
					} else {
						jtsDrawer.DrawLineString((LineString) geometry.geometry, geometry.z_layer, 1.2f,
							geometry.color, geometry.alpha);
					}
				} else if ( geometry.geometry.getGeometryType() == "Point" ) {
					if ( geometry.height > 0 ) {
						jtsDrawer.DrawSphere(geometry.agent.getLocation(), geometry.z_layer, geometry.height,
							geometry.color, geometry.alpha);
					} else {
						jtsDrawer.DrawPoint((Point) geometry.geometry, geometry.z_layer, 10,
							renderer.getMaxEnvDim() / 1000, geometry.color, geometry.alpha);
					}
				}
			}
			// Rotate angle (in XY plan)
			if ( geometry.angle != 0 ) {
				renderer.gl.glTranslated(geometry.geometry.getCentroid().getX(), this.jtsDrawer.yFlag *
					geometry.geometry.getCentroid().getY(), 0.0d);
				renderer.gl.glRotated(geometry.angle, 0.0d, 0.0d, 1.0d);
				renderer.gl.glTranslated(-geometry.geometry.getCentroid().getX(), -this.jtsDrawer.yFlag *
					geometry.geometry.getCentroid().getY(), 0.0d);

			}
		}
	}

	/**
	 * 
	 * The class CollectionDrawer.
	 * 
	 * @author drogoul
	 * @since 4 mai 2013
	 * 
	 */
	public static class CollectionDrawer extends ObjectDrawer<CollectionObject> {

		JTSDrawer jtsDrawer;

		public CollectionDrawer(JOGLAWTGLRenderer r) {
			super(r);
			jtsDrawer = new JTSDrawer(r);
		}

		@Override
		public void _draw(CollectionObject collection) {
			// Draw Shape file so need to inverse the y composante.
			jtsDrawer.yFlag = 1;
			renderer.gl.glPushMatrix();
			renderer.gl.glTranslated(-collection.collection.getBounds().centre().x, -collection.collection.getBounds()
				.centre().y, 0.0d);
			// Iterate throught all the collection
			SimpleFeatureIterator iterator = collection.collection.features();
			// Color color= Color.red;
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				Geometry sourceGeometry = (Geometry) feature.getDefaultGeometry();
				if ( sourceGeometry.getGeometryType() == "MultiPolygon" ) {
					jtsDrawer.DrawMultiPolygon((MultiPolygon) sourceGeometry, 0.0d, collection.color, 1.0d, true, null,
						0, 0.0d, false);
				} else if ( sourceGeometry.getGeometryType() == "Polygon" ) {
					jtsDrawer.DrawPolygon((Polygon) sourceGeometry, 0.0d, collection.color, 1.0d, true, null, false, 0,
						true, false);
				} else if ( sourceGeometry.getGeometryType() == "MultiLineString" ) {
					jtsDrawer.DrawMultiLineString((MultiLineString) sourceGeometry, 0.0d, collection.color, 1.0d, 0.0d);
				} else if ( sourceGeometry.getGeometryType() == "LineString" ) {
					jtsDrawer.DrawLineString((LineString) sourceGeometry, 0.0d, 1.0d, collection.color, 1.0d);
				} else if ( sourceGeometry.getGeometryType() == "Point" ) {
					jtsDrawer.DrawPoint((Point) sourceGeometry, 0.0d, 10, 10, collection.color, 1.0d);
				}
			}
			renderer.gl.glPopMatrix();
			jtsDrawer.yFlag = -1;
		}
	}

	/**
	 * 
	 * The class StringDrawer.
	 * 
	 * @author drogoul
	 * @since 4 mai 2013
	 * 
	 */

	public static class StringDrawer extends ObjectDrawer<StringObject> {

		// Setting it to true requires that the ModelScene handles strings outside a list (see ModelScene)
		static final boolean USE_VERTEX_ARRAYS = false;
		Map<String, Map<Integer, Map<Integer, TextRenderer>>> cache = new LinkedHashMap();

		// static String[] fonts = { "Helvetica", "Geneva", "Times", "Courier", "Arial", "Gill Sans", "Sans Serif" };
		// static int[] sizes = { 8, 10, 12, 14, 16, 20, 24, 36, 48, 60, 72 };
		// static int[] styles = { Font.PLAIN, Font.BOLD, Font.ITALIC };
		// static {
		// for ( String f : fonts ) {
		// for ( int s : sizes ) {
		// for ( int t : styles ) {
		// get(f, s, t);
		// }
		//
		// }
		// }
		// }

		TextRenderer get(String font, int size, int style) {
			Map<Integer, Map<Integer, TextRenderer>> map1 = cache.get(font);
			if ( map1 == null ) {
				map1 = new HashMap();
				cache.put(font, map1);
			}
			Map<Integer, TextRenderer> map2 = map1.get(size);
			if ( map2 == null ) {
				map2 = new HashMap();
				map1.put(size, map2);
			}
			TextRenderer r = map2.get(style);
			if ( r == null ) {
				r = new TextRenderer(new Font(font, style, size), true, true, null, true);
				r.setSmoothing(true);
				r.setUseVertexArrays(USE_VERTEX_ARRAYS);
				map2.put(style, r);
			}
			return r;
		}

		public StringDrawer(JOGLAWTGLRenderer r) {
			super(r);
		}

		protected void _drawOld(StringObject s) {
			renderer.gl.glDisable(GL_BLEND);
			renderer.gl.glColor4d(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), 1.0d);
			renderer.gl.glRasterPos3d(s.x, s.y, s.z + s.z_layer);
			renderer.gl.glScaled(8.0d, 8.0d, 8.0d);
			glut.glutBitmapString(GLUT.BITMAP_TIMES_ROMAN_10, s.string);
			renderer.gl.glScaled(0.125d, 0.125d, 0.125d);
			renderer.gl.glEnable(GL_BLEND);

		}

		@Override
		public void draw(StringObject object) {
			// renderer.gl.glTranslated(object.offset.x, -object.offset.y, object.offset.z);
			// renderer.gl.glScaled(object.scale.x, object.scale.y, 1);
			_draw(object);
			// renderer.gl.glScaled(1 / object.scale.x, 1 / object.scale.y, 1);
			// renderer.gl.glTranslated(-object.offset.x, object.offset.y, -object.offset.z);

		}

		@Override
		protected void _draw(StringObject s) {

			// FIXME height ?
			// renderer.getContext().makeCurrent();
			// GuiUtils.debug("ObjectDrawer.StringDrawer._draw env size " + renderer.getWidth() + " ;" +
			// renderer.getHeight());

			TextRenderer r = get(s.font, s.size, s.style);
			r.setColor(s.color);
			r.begin3DRendering();
			float x = (float) ((float) s.x * s.scale.x + s.offset.x);
			float y = (float) ((float) s.y * s.scale.y - s.offset.y);
			// GuiUtils.debug("ObjectDrawer.StringDrawer._draw '" + s.string + "' at " + x + " ; " + y + " [original: "
			// +
			// s.x + " ; " + s.y + "]" + "[offset: " + s.offset.x + " ; " + s.offset.y + "]");
			// renderer.gl.glTranslated(s.offset.x, -s.offset.y, s.offset.z);
			renderer.gl.glPushMatrix();
			// renderer.gl.glScaled(s.scale.x, s.scale.y, 1);
			r.draw3D(s.string, x, y, (float) (s.z + s.z_layer),
				(float) (/* s.scale.y * */renderer.env_height / renderer.getHeight()));
			// renderer.gl.glScaled(1 / s.scale.x, 1 / s.scale.y, 1);
			renderer.gl.glPopMatrix();
			// renderer.gl.glTranslated(-s.offset.x, s.offset.y, -s.offset.z);
			r.end3DRendering();
			// r.begin3DRendering();
			// r.draw3D(s.string, (float) s.x, (float) s.y, (float) (s.z + s.z_layer), s.alpha.floatValue());
			// r.flush();
			// r.end3DRendering();
			// renderer.getContext().release();
			// renderer.gl.glDisable(GL_BLEND);
			// renderer.gl.glColor4d(s.color.getRed(), s.color.getGreen(), s.color.getBlue(), 1.0d);
			// renderer.gl.glRasterPos3d(s.x, s.y, s.z + s.z_layer);
			// renderer.gl.glScaled(8.0d, 8.0d, 8.0d);
			// glut.glutBitmapString(GLUT.BITMAP_TIMES_ROMAN_10, s.string);
			// renderer.gl.glScaled(0.125d, 0.125d, 0.125d);
			// renderer.gl.glEnable(GL_BLEND);

		}

		@Override
		public void dispose() {
			cache.clear();
		}
	}

	public void dispose() {}

	public GL getGL() {
		return renderer.gl;
	}

}
