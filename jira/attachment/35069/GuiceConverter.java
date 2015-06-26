package guice.ser;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Injector;
import com.google.inject.cglib.proxy.Callback;
import com.google.inject.cglib.proxy.Enhancer;
import com.google.inject.cglib.proxy.Factory;
import com.google.inject.cglib.proxy.MethodInterceptor;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.CGLIBEnhancedConverter;
import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionProviderWrapper;
import com.thoughtworks.xstream.converters.reflection.SerializableConverter;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.CGLIBMapper;

/**
 * Converts a proxy created by the Guice {@link Enhancer}. Such a proxy is
 * recreated while deserializing the proxy. 
 * 
 * Note, that the this converter relies on the CGLIBMapper.
 * 
 * This converter is an adapted version of {@link CGLIBEnhancedConverter}
 * 
 * @author Anthony MÜLLER (anthony.muller@gmail.com)
 */
public class GuiceConverter extends SerializableConverter {

	// An alternative implementation is possible by using
	// Enhancer.setCallbackType and
	// Enhancer.createClass().
	// In this case the converter must be derived from the
	// AbstractReflectionConverter,
	// the proxy info must be written/read in a separate structure first, then
	// the
	// Enhancer must create the type and at last the functionality of the
	// ReflectionConverter
	// must be used to create the instance. But let's see user feedback first.
	// No support for multiple callbacks though ...

	private static String GUICE_NAMING_MARKER = "$$EnhancerByGuice$$";
	private static String CALLBACK_MARKER = "CGLIB$CALLBACK_";
	private transient Map fieldCache;
	private Injector injector;

	public GuiceConverter(XStream xstream, Injector injector) {
		super(xstream.getMapper(), new CGLIBFilteringReflectionProvider(xstream
				.getReflectionProvider()));
		this.fieldCache = new HashMap();
		this.injector = injector;
	}

	public boolean canConvert(Class type) {
		return (Enhancer.isEnhanced(type) && type.getName().indexOf(
				GUICE_NAMING_MARKER) > 0)
				|| type == CGLIBMapper.Marker.class;
	}

	public void marshal(Object source, HierarchicalStreamWriter writer,
			MarshallingContext context) {
		Class type = source.getClass();
		boolean hasFactory = Factory.class.isAssignableFrom(type);
		ExtendedHierarchicalStreamWriterHelper.startNode(writer, "type", type);
		context.convertAnother(type.getSuperclass());
		writer.endNode();

		// Do not write "interfaces", "hasFactory" as in CGLIBEnhancedConverter

		Callback[] callbacks = hasFactory ? ((Factory) source).getCallbacks()
				: getCallbacks(source);

		boolean isInterceptor = MethodInterceptor.class
				.isAssignableFrom(callbacks[0].getClass());

		try {
			final Field field = type.getDeclaredField("serialVersionUID");
			field.setAccessible(true);
			long serialVersionUID = field.getLong(null);
			ExtendedHierarchicalStreamWriterHelper.startNode(writer,
					"serialVersionUID", String.class);
			writer.setValue(String.valueOf(serialVersionUID));
			writer.endNode();
		} catch (NoSuchFieldException e) {
			// OK, ignore
		} catch (IllegalAccessException e) {
			// OK, ignore
		}
		if (isInterceptor && type.getSuperclass() != Object.class) {
			writer.startNode("instance");
			super.doMarshalConditionally(source, writer, context);
			writer.endNode();
		}
	}

	private Callback[] getCallbacks(Object source) {
		Class type = source.getClass();
		List fields = (List) fieldCache.get(type.getName());
		if (fields == null) {
			fields = new ArrayList();
			fieldCache.put(type.getName(), fields);
			for (int i = 0; true; ++i) {
				try {
					Field field = type.getDeclaredField(CALLBACK_MARKER + i);
					field.setAccessible(true);
					fields.add(field);
				} catch (NoSuchFieldException e) {
					break;
				}
			}
		}
		List list = new ArrayList();
		for (int i = 0; i < fields.size(); ++i) {
			try {
				Field field = (Field) fields.get(i);
				list.add(field.get(source));
			} catch (IllegalAccessException e) {
				throw new ObjectAccessException("Access to " + type.getName()
						+ "." + CALLBACK_MARKER + i + " not allowed");
			}
		}
		return (Callback[]) list.toArray(new Callback[list.size()]);
	}

	public Object unmarshal(HierarchicalStreamReader reader,
			UnmarshallingContext context) {
		reader.moveDown();

		Class realClazz = (Class) context.convertAnother(null, Class.class);
		reader.moveUp();

		Object result = null;
		while (reader.hasMoreChildren()) {
			reader.moveDown();
			if (reader.getNodeName().equals("instance")) {
				result = injector.getInstance(realClazz);
				super.doUnmarshalConditionally(result, reader, context);
			}
			reader.moveUp();
		}
		return serializationMethodInvoker
				.callReadResolve(result == null ? injector
						.getInstance(realClazz) : result);
	}

	protected List hierarchyFor(Class type) {
		List typeHierarchy = super.hierarchyFor(type);
		// drop the CGLIB proxy
		typeHierarchy.remove(typeHierarchy.size() - 1);
		return typeHierarchy;
	}

	private Object readResolve() {
		fieldCache = new HashMap();
		return this;
	}

	private static class CGLIBFilteringReflectionProvider extends
			ReflectionProviderWrapper {

		public CGLIBFilteringReflectionProvider(
				final ReflectionProvider reflectionProvider) {
			super(reflectionProvider);
		}

		public void visitSerializableFields(final Object object,
				final Visitor visitor) {
			wrapped.visitSerializableFields(object, new Visitor() {
				public void visit(String name, Class type, Class definedIn,
						Object value) {
					if (!name.startsWith("CGLIB$")) {
						visitor.visit(name, type, definedIn, value);
					}
				}
			});
		}
	}
}
