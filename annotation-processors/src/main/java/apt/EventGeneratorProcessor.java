package apt;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

/**
 * Генерирует аспекты, добавляюще ивенты по аннотации.
 *
 * @author p.yushkovskiy
 */
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public final class EventGeneratorProcessor extends AbstractProcessor {

  @NotNull
  private final Map<TypeElement, List<Item>> collectedElements = new HashMap<>();

  @NotNull
  private static String callbacksClassName(@NotNull TypeElement ce) {
    return ce.getSimpleName() + "Callbacks";
  }

  @NotNull
  private static String callbacksSubClassName(@NotNull Item item) {
    return "On" + capitalize(item.element.getSimpleName().toString());
  }

  @NotNull
  private static String callbackName(@NotNull TypeElement ce, @NotNull Item item) {
    return callbacksClassName(ce) + '.' + callbacksSubClassName(item);
  }

  @NotNull
  private static String eventName(@NotNull TypeElement ce, @NotNull Item item) {
    return ce.getSimpleName() + fieldName(item);
  }

  @NotNull
  private static String fieldName(@NotNull Item item) {
    return "On" + capitalize(item.element.getSimpleName().toString()) + "Event";
  }

  private static String capitalize(String str) {
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }

  private static String toString(@NotNull VariableElement var, boolean printNames) {
    final StringBuilder builder = new StringBuilder();
    if (printNames) {
      for (final AnnotationMirror annotation : var.getAnnotationMirrors())
        builder.append(annotation).append(' ');
    }
    builder.append(var.asType());
    if (printNames)
      builder.append(' ').append(var);

    return builder.toString();
  }

  private static void generateEmit(@NotNull BufferedWriter bw, @NotNull TypeElement ce, @NotNull Item item) throws IOException {
    bw.append("  private static void emit(@NotNull ").append(eventName(ce, item)).append(" event, @NotNull ").append(ce.getSimpleName()).append(" emmiter");
    for (final VariableElement variableElement : item.element.getParameters())
      bw.append(", ").append(toString(variableElement, true));
    bw.append(") {\n");
    bw.append("    final Collection<").append(callbackName(ce, item)).append("> callbacksSafe = event.callbacks;\n");
    bw.append("    if (callbacksSafe == null)\n");
    bw.append("      return;\n");
    bw.append("    for (final ").append(callbackName(ce, item)).append(" callback : new ArrayList<>(callbacksSafe))\n");
    bw.append("      callback.changed(emmiter");
    for (final VariableElement variableElement : item.element.getParameters())
      bw.append(", ").append(variableElement.getSimpleName());
    bw.append(");\n");
    bw.append("  }\n");
    bw.newLine();
  }

  private static void generateEvent(@NotNull BufferedWriter bw, @NotNull TypeElement ce, @NotNull Item item) throws IOException {
    bw.append("  public static final class ").append(eventName(ce, item)).append(" {\n");
    bw.append("    @Nullable\n");
    bw.append("    private Collection<").append(callbackName(ce, item)).append("> callbacks = null;\n");
    bw.append("\n");
    bw.append("    ").append(eventName(ce, item)).append("() {\n");
    bw.append("    }\n");
    bw.append("\n");
    bw.append("    public void add(@NotNull ").append(callbackName(ce, item)).append(" callback) {\n");
    bw.append("      Collection<").append(callbackName(ce, item)).append("> callbacksSafe = callbacks;\n");
    bw.append("      if (callbacksSafe == null) {\n");
    bw.append("        callbacksSafe = new ArrayList<>(1);\n");
    bw.append("        callbacks = callbacksSafe;\n");
    bw.append("      }\n");
    bw.append("      callbacksSafe.add(callback);\n");
    bw.append("    }\n");
    bw.append("\n");
    bw.append("    public void clean() {\n");
    bw.append("      callbacks = null;\n");
    bw.append("    }\n");
    bw.append("  }");
    bw.newLine();
    bw.newLine();
  }

  private static void generateField(@NotNull BufferedWriter bw, @NotNull TypeElement ce, @NotNull Item item) throws IOException {
    bw.append("  @SuppressWarnings(\"PublicField\")\n");
    bw.append("  public final ").append(eventName(ce, item)).append(' ').append(ce.getQualifiedName()).append('.').append(fieldName(item))
        .append(" = new ").append(eventName(ce, item)).append("();\n");
    bw.newLine();
  }

  private static void generatePointcut(@NotNull BufferedWriter bw, @NotNull TypeElement ce, @NotNull Item item) throws IOException {
    bw.append("  ").append(item.description.value().name().toLowerCase()).append("(): execution(").append(item.element.getReturnType().toString()).append(' ').append(ce.getQualifiedName()).append('.')
        .append(item.element.getSimpleName()).append('(');
    for (final Iterator<? extends TypeParameterElement> i = item.element.getTypeParameters().iterator(); i.hasNext(); ) {
      bw.append(i.next().getSimpleName());
      if (i.hasNext())
        bw.append(", ");
    }
    bw.append(")) {\n");
    bw.append("    final ").append(ce.getQualifiedName()).append(" emmiter = (").append(ce.getQualifiedName()).append(") thisJoinPoint.getThis();\n");
    bw.append("    emit(emmiter.").append(fieldName(item)).append(", emmiter");
    final List<? extends VariableElement> parameters = item.element.getParameters();
    for (int i = 0, s = parameters.size(); i < s; i++) {
      final VariableElement element = parameters.get(i);
      final TypeMirror type = element.asType();
      bw.append(", ").append('(').append(type.toString()).append(") thisJoinPoint.getArgs()[").append(Integer.toString(i)).append(']');
    }
    bw.append(");\n");
    bw.append("  }");
    bw.newLine();
    bw.newLine();
  }

  @Override
  @NotNull
  public Set<String> getSupportedAnnotationTypes() {
    return Collections.singleton(Event.class.getName());
  }

  @Override
  public boolean process(@NotNull Set<? extends TypeElement> annotations, @NotNull RoundEnvironment roundEnv) {
    if (roundEnv.processingOver()) {
      generateAll();
      return true;
    }

    for (final Element elem : roundEnv.getElementsAnnotatedWith(Event.class)) {
      final Event description = elem.getAnnotation(Event.class);
      if (description == null)
        continue;

      if (elem.getKind() == ElementKind.FIELD) {
        // TODO(yushkovskiy): implement this
        continue;
      }

      if (elem.getKind() != ElementKind.METHOD)
        continue;

      final ExecutableElement exeElement = (ExecutableElement) elem;
      final Element enclosingElement = exeElement.getEnclosingElement();
      final TypeElement classElement = (TypeElement) enclosingElement;

      List<Item> items = collectedElements.get(classElement);
      if (items == null) {
        items = new ArrayList<>();
        collectedElements.put(classElement, items);
      }
      items.add(new Item(exeElement, description));
    }

    return true; // no further processing of this annotation type
  }

  private void generateCallbacks(@NotNull TypeElement ce, @NotNull List<Item> items) throws IOException {
    final PackageElement packageElement = (PackageElement) ce.getEnclosingElement();
    final JavaFileObject jfo = processingEnv.getFiler().createSourceFile(packageElement.getQualifiedName().toString() + '.' + callbacksClassName(ce));
    try (final BufferedWriter bw = new BufferedWriter(jfo.openWriter())) {
      bw.append("package ").append(packageElement.getQualifiedName()).append(";");
      bw.newLine();
      bw.newLine();
      bw.append("import org.jetbrains.annotations.NotNull;\n");
      bw.append("import org.jetbrains.annotations.Nullable;\n");
      bw.newLine();
      bw.newLine();
      bw.append("/**\n").append(" * Events' callbacks for ").append(ce.getQualifiedName()).append(".\n").append(" *\n").append(" * @author ").append(EventGeneratorProcessor.class.getCanonicalName()).append("\n").append(" */");
      bw.newLine();

      bw.append("@").append(Generated.class.getCanonicalName()).append("(\"").append(EventGeneratorProcessor.class.getCanonicalName()).append("\")");
      bw.newLine();
      bw.append("public final class ").append(callbacksClassName(ce)).append(" {");
      bw.newLine();

      for (final Item item : items) {
        bw.append("  public interface ").append(callbacksSubClassName(item)).append(" {\n");
        bw.append("    void changed(@NotNull ").append(ce.getSimpleName()).append(" emmiter");
        for (final VariableElement var : item.element.getParameters())
          bw.append(", ").append(toString(var, true));
        bw.append(");\n");
        bw.append("  }");
        bw.newLine();
        bw.newLine();
      }

      bw.append("}");
      bw.newLine();
      bw.newLine();
    }
  }

  private void generateAll() {
    for (final Map.Entry<TypeElement, List<Item>> entry : collectedElements.entrySet()) {
      final TypeElement classElement = entry.getKey();
      final PackageElement packageElement = (PackageElement) classElement.getEnclosingElement();
      try {
        final FileObject jfo = processingEnv.getFiler().createResource(
            StandardLocation.SOURCE_OUTPUT,
            packageElement.getQualifiedName(),
            classElement.getSimpleName() + "EventsAspect.aj");

        try (final BufferedWriter bw = new BufferedWriter(jfo.openWriter())) {
          bw.append("package ").append(packageElement.getQualifiedName()).append(";");
          bw.newLine();
          bw.newLine();
          bw.append("import org.jetbrains.annotations.NotNull;\n");
          bw.append("import org.jetbrains.annotations.Nullable;\n");
          bw.newLine();
          bw.append("import java.util.ArrayList;\n");
          bw.append("import java.util.Collection;");
          bw.newLine();
          bw.newLine();
          bw.append("/**\n").append(" * Events for ").append(classElement.getQualifiedName()).append(".\n").append(" *\n").append(" * @author ").append(EventGeneratorProcessor.class.getCanonicalName()).append("\n").append(" */");
          bw.newLine();
          bw.append("@").append(Generated.class.getCanonicalName()).append("(\"").append(EventGeneratorProcessor.class.getCanonicalName()).append("\")");
          bw.newLine();
          bw.append("final aspect ").append(classElement.getSimpleName()).append("EventsAspect").append(" {");
          bw.newLine();
          bw.newLine();

          generateCallbacks(classElement, entry.getValue());
          for (final Item item : entry.getValue()) {
            generateEvent(bw, classElement, item);
            generateEmit(bw, classElement, item);
            generateField(bw, classElement, item);
            generatePointcut(bw, classElement, item);
          }

          bw.append("}");
          bw.newLine();
          bw.newLine();
        }
      } catch (final Throwable e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
      }
    }
  }

  private static final class Item {
    @NotNull
    private final ExecutableElement element;
    @NotNull
    private final Event description;

    private Item(@NotNull ExecutableElement element, @NotNull Event description) {
      this.element = element;
      this.description = description;
    }
  }
}