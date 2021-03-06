package openperipheral.util;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Collection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import openmods.Log;
import openperipheral.adapter.IMethodDescription;
import openperipheral.adapter.IMethodDescription.IArgumentDescription;
import openperipheral.adapter.IMethodExecutor;
import openperipheral.adapter.PeripheralTypeProvider;
import openperipheral.adapter.composed.IMethodMap;
import openperipheral.adapter.composed.IMethodMap.IMethodVisitor;
import openperipheral.adapter.types.TypeHelper;
import openperipheral.adapter.wrappers.AdapterWrapper;
import openperipheral.api.adapter.AdapterSourceName;
import openperipheral.api.adapter.IScriptType;
import openperipheral.api.architecture.IFeatureGroupManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DocBuilder {
	private final Document doc;
	private final Element root;

	public interface IClassDecorator {
		public void decorateEntry(Element element, Class<?> cls);
	}

	public static final IClassDecorator NULL_DECORATOR = new IClassDecorator() {
		@Override
		public void decorateEntry(Element element, Class<?> cls) {}
	};

	public static final IClassDecorator SCRIPT_OBJECT_DECORATOR = new IClassDecorator() {
		@Override
		public void decorateEntry(Element element, Class<?> cls) {
			final AdapterSourceName nameAnnotation = cls.getAnnotation(AdapterSourceName.class);
			final String name = nameAnnotation != null? nameAnnotation.value() : cls.getSimpleName().toLowerCase();

			Document doc = element.getOwnerDocument();
			element.appendChild(createProperty(doc, "name", name));
		}
	};

	public static final IClassDecorator TILE_ENTITY_DECORATOR = new IClassDecorator() {

		@Override
		public void decorateEntry(Element element, Class<?> cls) {
			final String teName = Objects.firstNonNull(NameUtils.getClassToNameMap().get(cls), "null");
			Document doc = element.getOwnerDocument();
			element.appendChild(createProperty(doc, "teName", teName));

			String docText = DocUtils.DOC_TEXT_CACHE.getOrCreate(cls);
			if (!Strings.isNullOrEmpty(docText)) element.appendChild(createCDataProperty(doc, "docText", docText));

			String userName = PeripheralTypeProvider.INSTANCE.getType(cls);
			if (Strings.isNullOrEmpty(userName)) {
				userName = "unknown?";
			}
			element.appendChild(createProperty(doc, "name", userName));
		}
	};

	public DocBuilder() {
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.newDocument();
			root = doc.createElement("documentation");
			doc.appendChild(root);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public void dumpXml(File output, boolean applyXslt) {
		try {
			final Transformer transformer = createTransformer(applyXslt);
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(output);
			transformer.transform(source, result);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	private Transformer createTransformer(boolean applyXslt) throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();

		if (!applyXslt) return transformerFactory.newTransformer();

		InputStream stylesheet = getClass().getResourceAsStream("/op_dump.xsl");
		try {
			StreamSource stylesource = new StreamSource(stylesheet);
			return transformerFactory.newTransformer(stylesource);
		} finally {
			stylesheet.close();
		}
	}

	public void createDocForClass(String architecture, String type, IClassDecorator decorator, Class<?> cls, IMethodMap methods) {
		if (methods.isEmpty()) return;
		Element result = doc.createElement("classMethods");
		result.setAttribute("type", type);
		result.setAttribute("architecture", architecture);
		fillDocForClass(result, cls, methods);
		decorator.decorateEntry(result, cls);
		root.appendChild(result);
	}

	public void createDocForFeatureGroup(String id, Collection<String> architectures, IFeatureGroupManager fgm) {
		Element result = doc.createElement("featureGroup");
		result.setAttribute("id", id);

		for (String arch : architectures)
			if (fgm.isEnabled(id, arch)) result.appendChild(createProperty("architecture", arch));

		root.appendChild(result);
	}

	public void createDocForAdapter(String type, String location, Class<?> targetClass, AdapterWrapper adapter) {
		Element result = doc.createElement("adapter");
		final Class<?> adapterClass = adapter.getAdapterClass();
		result.setAttribute("class", adapterClass.getName());
		result.setAttribute("source", getSourceFile(adapterClass));
		result.setAttribute("type", type);
		result.setAttribute("location", location);

		result.appendChild(createProperty("target", adapter.getTargetClass().getName()));
		result.appendChild(createProperty("source", adapter.source()));

		fillMethods(result, adapter.getMethods());
		root.appendChild(result);
	}

	public void createDocForArchitecture(String id, boolean isEnabled) {
		Element result = doc.createElement("architecture");
		result.appendChild(createProperty("id", id));
		result.setAttribute("enabled", Boolean.toString(isEnabled));

		root.appendChild(result);
	}

	public void setRootAttribute(String name, String value) {
		root.setAttribute(name, value);
	}

	private static String getSourceFile(Class<?> adapterClass) {
		try {
			final ProtectionDomain protectionDomain = adapterClass.getProtectionDomain();
			if (protectionDomain == null) return "none";
			URL sourceUrl = protectionDomain.getCodeSource().getLocation();
			return sourceUrl != null? sourceUrl.toString() : "none";
		} catch (Throwable e) {
			Log.warn(e, "Failed to get source for class %s", adapterClass);
		}
		return "unknown";
	}

	protected void fillMethods(Element result, Collection<? extends IMethodExecutor> methods) {
		for (IMethodExecutor method : methods) {
			Element methodDoc = doc.createElement("method");

			final IMethodDescription description = method.description();

			Element names = doc.createElement("names");
			for (String name : description.getNames())
				names.appendChild(createProperty("name", name));
			methodDoc.appendChild(names);

			fillDocForMethod(methodDoc, method);
			result.appendChild(methodDoc);
		}
	}

	private void fillDocForClass(final Element result, Class<?> cls, IMethodMap list) {
		result.setAttribute("class", cls.getName());
		result.appendChild(createProperty("simpleName", cls.getSimpleName()));

		list.visitMethods(new IMethodVisitor() {
			@Override
			public void visit(String name, IMethodExecutor executor) {
				Element methodDoc = doc.createElement("method");

				methodDoc.setAttribute("name", name);
				fillDocForMethod(methodDoc, executor);
				result.appendChild(methodDoc);
			}
		});
	}

	private void fillDocForMethod(Element result, IMethodExecutor method) {
		result.setAttribute("asynchronous", Boolean.toString(method.isAsynchronous()));
		final Optional<String> returnSignal = method.getReturnSignal();
		if (returnSignal.isPresent()) result.setAttribute("returnSignal", returnSignal.get());
		IMethodDescription description = method.description();
		result.appendChild(createProperty("signature", DocUtils.signature(description)));
		result.appendChild(createProperty("source", description.source()));

		addOptionalTag(result, "description", description.description());

		{
			for (String fg : method.featureGroups())
				result.appendChild(createProperty("featureGroup", fg));
		}

		{
			Element args = doc.createElement("arguments");
			for (IArgumentDescription arg : description.arguments())
				args.appendChild(fillDocForArg(arg));
			result.appendChild(args);
		}

		{
			final IScriptType returnType = description.returnTypes();
			if (!TypeHelper.isVoid(returnType)) {
				final String returnTypes = returnType.describe();
				result.appendChild(createProperty("returns", returnTypes));
			}
		}
	}

	private Element fillDocForArg(IArgumentDescription arg) {
		Element result = doc.createElement("arg");
		result.appendChild(createProperty("name", arg.name()));
		result.appendChild(createProperty("type", arg.type().describe()));

		addOptionalTag(result, "description", arg.description());

		result.setAttribute("nullable", Boolean.toString(arg.nullable()));
		result.setAttribute("optional", Boolean.toString(arg.optional()));
		result.setAttribute("variadic", Boolean.toString(arg.variadic()));

		return result;
	}

	private void addOptionalTag(Element parent, final String tag, final String value) {
		if (!value.isEmpty()) parent.appendChild(createProperty(tag, value));
	}

	private Element createProperty(String tag, String value) {
		return createProperty(doc, tag, value);
	}

	private static Element createProperty(Document doc, String tag, String value) {
		Element el = doc.createElement(tag);
		el.appendChild(doc.createTextNode(value));
		return el;
	}

	private static Element createCDataProperty(Document doc, String tag, String value) {
		Element el = doc.createElement(tag);
		el.appendChild(doc.createCDATASection(value));
		return el;
	}
}
