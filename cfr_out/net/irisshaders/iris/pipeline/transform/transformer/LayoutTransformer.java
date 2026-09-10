/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.github.douira.glsl_transformer.ast.data.ChildNodeList
 *  io.github.douira.glsl_transformer.ast.node.Identifier
 *  io.github.douira.glsl_transformer.ast.node.TranslationUnit
 *  io.github.douira.glsl_transformer.ast.node.abstract_node.ASTNode
 *  io.github.douira.glsl_transformer.ast.node.declaration.DeclarationMember
 *  io.github.douira.glsl_transformer.ast.node.declaration.TypeAndInitDeclaration
 *  io.github.douira.glsl_transformer.ast.node.expression.Expression
 *  io.github.douira.glsl_transformer.ast.node.expression.LiteralExpression
 *  io.github.douira.glsl_transformer.ast.node.external_declaration.DeclarationExternalDeclaration
 *  io.github.douira.glsl_transformer.ast.node.external_declaration.ExternalDeclaration
 *  io.github.douira.glsl_transformer.ast.node.type.qualifier.LayoutQualifier
 *  io.github.douira.glsl_transformer.ast.node.type.qualifier.NamedLayoutQualifierPart
 *  io.github.douira.glsl_transformer.ast.node.type.qualifier.StorageQualifier
 *  io.github.douira.glsl_transformer.ast.node.type.qualifier.StorageQualifier$StorageType
 *  io.github.douira.glsl_transformer.ast.node.type.qualifier.TypeQualifier
 *  io.github.douira.glsl_transformer.ast.node.type.qualifier.TypeQualifierPart
 *  io.github.douira.glsl_transformer.ast.node.type.specifier.BuiltinNumericTypeSpecifier
 *  io.github.douira.glsl_transformer.ast.node.type.specifier.TypeSpecifier
 *  io.github.douira.glsl_transformer.ast.query.Root
 *  io.github.douira.glsl_transformer.ast.query.match.Matcher
 *  io.github.douira.glsl_transformer.ast.transform.ASTInjectionPoint
 *  io.github.douira.glsl_transformer.ast.transform.ASTParser
 *  io.github.douira.glsl_transformer.ast.transform.Template
 *  io.github.douira.glsl_transformer.parser.ParseShape
 *  io.github.douira.glsl_transformer.util.Type
 *  it.unimi.dsi.fastutil.objects.Object2IntArrayMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package net.irisshaders.iris.pipeline.transform.transformer;

import io.github.douira.glsl_transformer.ast.data.ChildNodeList;
import io.github.douira.glsl_transformer.ast.node.Identifier;
import io.github.douira.glsl_transformer.ast.node.TranslationUnit;
import io.github.douira.glsl_transformer.ast.node.abstract_node.ASTNode;
import io.github.douira.glsl_transformer.ast.node.declaration.DeclarationMember;
import io.github.douira.glsl_transformer.ast.node.declaration.TypeAndInitDeclaration;
import io.github.douira.glsl_transformer.ast.node.expression.Expression;
import io.github.douira.glsl_transformer.ast.node.expression.LiteralExpression;
import io.github.douira.glsl_transformer.ast.node.external_declaration.DeclarationExternalDeclaration;
import io.github.douira.glsl_transformer.ast.node.external_declaration.ExternalDeclaration;
import io.github.douira.glsl_transformer.ast.node.type.qualifier.LayoutQualifier;
import io.github.douira.glsl_transformer.ast.node.type.qualifier.NamedLayoutQualifierPart;
import io.github.douira.glsl_transformer.ast.node.type.qualifier.StorageQualifier;
import io.github.douira.glsl_transformer.ast.node.type.qualifier.TypeQualifier;
import io.github.douira.glsl_transformer.ast.node.type.qualifier.TypeQualifierPart;
import io.github.douira.glsl_transformer.ast.node.type.specifier.BuiltinNumericTypeSpecifier;
import io.github.douira.glsl_transformer.ast.node.type.specifier.TypeSpecifier;
import io.github.douira.glsl_transformer.ast.query.Root;
import io.github.douira.glsl_transformer.ast.query.match.Matcher;
import io.github.douira.glsl_transformer.ast.transform.ASTInjectionPoint;
import io.github.douira.glsl_transformer.ast.transform.ASTParser;
import io.github.douira.glsl_transformer.ast.transform.Template;
import io.github.douira.glsl_transformer.parser.ParseShape;
import io.github.douira.glsl_transformer.util.Type;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.gl.shader.ShaderType;
import net.irisshaders.iris.pipeline.transform.PatchShaderType;
import net.irisshaders.iris.pipeline.transform.parameter.Parameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LayoutTransformer {
    private static final Logger LOGGER = LogManager.getLogger(LayoutTransformer.class);
    private static final ShaderType[] pipeline = new ShaderType[]{ShaderType.VERTEX, ShaderType.TESSELATION_CONTROL, ShaderType.TESSELATION_EVAL, ShaderType.GEOMETRY, ShaderType.FRAGMENT};
    private static final Matcher<ExternalDeclaration> outDeclarationMatcher = new DeclarationMatcher(StorageQualifier.StorageType.OUT);
    private static final Matcher<ExternalDeclaration> inDeclarationMatcher = new DeclarationMatcher(StorageQualifier.StorageType.IN);
    private static final Matcher<ExternalDeclaration> nonLayoutOutDeclarationMatcher = new Matcher<ExternalDeclaration>("out float name;", ParseShape.EXTERNAL_DECLARATION){
        {
            this.markClassWildcard("qualifier", ((ExternalDeclaration)this.pattern).getRoot().nodeIndex.getUnique(TypeQualifier.class));
            this.markClassWildcard("type", ((ExternalDeclaration)this.pattern).getRoot().nodeIndex.getUnique(BuiltinNumericTypeSpecifier.class));
            this.markClassWildcard("name*", ((Identifier)((ExternalDeclaration)this.pattern).getRoot().identifierIndex.getUnique("name")).getAncestor(DeclarationMember.class));
        }

        public boolean matchesExtract(ExternalDeclaration tree) {
            boolean result = super.matchesExtract((ASTNode)tree);
            if (!result) {
                return false;
            }
            TypeQualifier qualifier = (TypeQualifier)this.getNodeMatch("qualifier", TypeQualifier.class);
            boolean hasOutQualifier = false;
            for (TypeQualifierPart part : qualifier.getParts()) {
                if (part instanceof StorageQualifier) {
                    StorageQualifier storageQualifier = (StorageQualifier)part;
                    if (storageQualifier.storageType != StorageQualifier.StorageType.OUT) continue;
                    hasOutQualifier = true;
                    continue;
                }
                if (!(part instanceof LayoutQualifier)) continue;
                return false;
            }
            return hasOutQualifier;
        }
    };
    private static final Matcher<ExternalDeclaration> nonLayoutInDeclarationMatcher = new Matcher<ExternalDeclaration>("in float name;", ParseShape.EXTERNAL_DECLARATION){
        {
            this.markClassWildcard("qualifier", ((ExternalDeclaration)this.pattern).getRoot().nodeIndex.getUnique(TypeQualifier.class));
            this.markClassWildcard("type", ((ExternalDeclaration)this.pattern).getRoot().nodeIndex.getUnique(BuiltinNumericTypeSpecifier.class));
            this.markClassWildcard("name*", ((Identifier)((ExternalDeclaration)this.pattern).getRoot().identifierIndex.getUnique("name")).getAncestor(DeclarationMember.class));
        }

        public boolean matchesExtract(ExternalDeclaration tree) {
            boolean result = super.matchesExtract((ASTNode)tree);
            if (!result) {
                return false;
            }
            TypeQualifier qualifier = (TypeQualifier)this.getNodeMatch("qualifier", TypeQualifier.class);
            boolean hasOutQualifier = false;
            for (TypeQualifierPart part : qualifier.getParts()) {
                if (part instanceof StorageQualifier) {
                    StorageQualifier storageQualifier = (StorageQualifier)part;
                    if (storageQualifier.storageType != StorageQualifier.StorageType.IN) continue;
                    hasOutQualifier = true;
                    continue;
                }
                if (!(part instanceof LayoutQualifier)) continue;
                return false;
            }
            return hasOutQualifier;
        }
    };
    private static final Template<ExternalDeclaration> layoutedOutDeclarationTemplate = Template.withExternalDeclaration((String)"out __type __name;");
    private static final Template<ExternalDeclaration> layoutedInDeclarationTemplate = Template.withExternalDeclaration((String)"in __type __name;");
    private static final String attachTargetPrefix = "outColor";
    private static final List<String> reservedWords = List.of("texture");

    private static StorageQualifier getConstQualifier(TypeQualifier qualifier) {
        if (qualifier == null) {
            return null;
        }
        for (TypeQualifierPart constQualifier : qualifier.getChildren()) {
            if (!(constQualifier instanceof StorageQualifier)) continue;
            StorageQualifier storageQualifier = (StorageQualifier)constQualifier;
            if (storageQualifier.storageType != StorageQualifier.StorageType.CONST) continue;
            return storageQualifier;
        }
        return null;
    }

    private static TypeQualifier makeQualifierOut(TypeQualifier typeQualifier) {
        for (TypeQualifierPart qualifierPart : typeQualifier.getParts()) {
            if (!(qualifierPart instanceof StorageQualifier)) continue;
            StorageQualifier storageQualifier = (StorageQualifier)qualifierPart;
            if (storageQualifier.storageType != StorageQualifier.StorageType.IN) continue;
            storageQualifier.storageType = StorageQualifier.StorageType.OUT;
        }
        return typeQualifier;
    }

    public static void transformGrouped(ASTParser t, Map<PatchShaderType, TranslationUnit> trees, Parameters parameters) {
        Object prevType = null;
        AtomicReference lastMap = new AtomicReference();
        for (ShaderType type : pipeline) {
            TranslationUnit currentTree;
            PatchShaderType[] patchTypes = PatchShaderType.fromGlShaderType(type);
            boolean hasAny = false;
            for (PatchShaderType currentType : patchTypes) {
                if (trees.get((Object)currentType) == null) continue;
                hasAny = true;
            }
            if (!hasAny || (currentTree = trees.get((Object)patchTypes[0])) == null) continue;
            Root currentRoot = currentTree.getRoot();
            currentRoot.indexBuildSession(root -> {
                if (root != null) {
                    if (lastMap.get() != null) {
                        LayoutTransformer.transformIn((Object2IntMap<String>)((Object2IntMap)lastMap.get()), t, currentTree, root, parameters);
                    }
                    lastMap.set(LayoutTransformer.transformOut(t, currentTree, root, parameters));
                }
            });
        }
    }

    public static Object2IntMap<String> transformOut(ASTParser t, TranslationUnit tree, Root root, Parameters parameters) {
        ArrayList<NewDeclarationData> newDeclarationData = new ArrayList<NewDeclarationData>();
        int location = 0;
        Object2IntArrayMap map = new Object2IntArrayMap();
        ArrayList<DeclarationExternalDeclaration> declarationsToRemove = new ArrayList<DeclarationExternalDeclaration>();
        for (DeclarationExternalDeclaration declaration : root.nodeIndex.get(DeclarationExternalDeclaration.class)) {
            if (!nonLayoutOutDeclarationMatcher.matchesExtract((ASTNode)declaration)) continue;
            ChildNodeList members = ((TypeAndInitDeclaration)((DeclarationMember)nonLayoutOutDeclarationMatcher.getNodeMatch("name*", DeclarationMember.class)).getAncestor(TypeAndInitDeclaration.class)).getMembers();
            TypeQualifier typeQualifier = (TypeQualifier)nonLayoutOutDeclarationMatcher.getNodeMatch("qualifier", TypeQualifier.class);
            BuiltinNumericTypeSpecifier typeSpecifier = (BuiltinNumericTypeSpecifier)nonLayoutOutDeclarationMatcher.getNodeMatch("type", BuiltinNumericTypeSpecifier.class);
            int addedDeclarations = 0;
            for (DeclarationMember member : members) {
                String name = member.getName().getName();
                map.put((Object)name, location);
                Iris.logger.warn("Found a declaration named " + name);
                newDeclarationData.add(new NewDeclarationData(typeQualifier, (TypeSpecifier)typeSpecifier, member, location++, name));
                ++addedDeclarations;
            }
            if (addedDeclarations != members.size()) continue;
            declarationsToRemove.add(declaration);
        }
        tree.getChildren().removeAll(declarationsToRemove);
        for (DeclarationExternalDeclaration declaration : declarationsToRemove) {
            declaration.detachParent();
        }
        ArrayList<ExternalDeclaration> newDeclarations = new ArrayList<ExternalDeclaration>();
        for (NewDeclarationData data : newDeclarationData) {
            DeclarationMember member = data.member;
            member.detach();
            TypeQualifier newQualifier = data.qualifier.cloneInto(root);
            newQualifier.getChildren().add(0, (Object)new LayoutQualifier(Stream.of(new NamedLayoutQualifierPart(new Identifier("location"), (Expression)new LiteralExpression(Type.INT32, (long)data.location)))));
            ExternalDeclaration newDeclaration = (ExternalDeclaration)layoutedOutDeclarationTemplate.getInstanceFor(root, new ASTNode[]{newQualifier, data.type.cloneInto(root), member});
            newDeclarations.add(newDeclaration);
        }
        tree.injectNodes(ASTInjectionPoint.BEFORE_DECLARATIONS, newDeclarations);
        return map;
    }

    public static void transformIn(Object2IntMap<String> map, ASTParser t, TranslationUnit tree, Root root, Parameters parameters) {
        ArrayList<NewDeclarationData> newDeclarationData = new ArrayList<NewDeclarationData>();
        ArrayList<DeclarationExternalDeclaration> declarationsToRemove = new ArrayList<DeclarationExternalDeclaration>();
        for (DeclarationExternalDeclaration declaration : root.nodeIndex.get(DeclarationExternalDeclaration.class)) {
            if (!nonLayoutInDeclarationMatcher.matchesExtract((ASTNode)declaration)) continue;
            ChildNodeList members = ((TypeAndInitDeclaration)((DeclarationMember)nonLayoutInDeclarationMatcher.getNodeMatch("name*", DeclarationMember.class)).getAncestor(TypeAndInitDeclaration.class)).getMembers();
            TypeQualifier typeQualifier = (TypeQualifier)nonLayoutInDeclarationMatcher.getNodeMatch("qualifier", TypeQualifier.class);
            BuiltinNumericTypeSpecifier typeSpecifier = (BuiltinNumericTypeSpecifier)nonLayoutInDeclarationMatcher.getNodeMatch("type", BuiltinNumericTypeSpecifier.class);
            int addedDeclarations = 0;
            for (DeclarationMember member : members) {
                String name = member.getName().getName();
                Iris.logger.warn("Found a member with name " + name);
                if (!map.containsKey((Object)name)) continue;
                newDeclarationData.add(new NewDeclarationData(typeQualifier, (TypeSpecifier)typeSpecifier, member, map.getInt((Object)name), name));
                ++addedDeclarations;
            }
            if (addedDeclarations != members.size()) continue;
            declarationsToRemove.add(declaration);
        }
        tree.getChildren().removeAll(declarationsToRemove);
        for (DeclarationExternalDeclaration declaration : declarationsToRemove) {
            declaration.detachParent();
        }
        ArrayList<ExternalDeclaration> newDeclarations = new ArrayList<ExternalDeclaration>();
        for (NewDeclarationData data : newDeclarationData) {
            DeclarationMember member = data.member;
            member.detach();
            TypeQualifier newQualifier = data.qualifier.cloneInto(root);
            newQualifier.getChildren().add(0, (Object)new LayoutQualifier(Stream.of(new NamedLayoutQualifierPart(new Identifier("location"), (Expression)new LiteralExpression(Type.INT32, (long)data.location)))));
            ExternalDeclaration newDeclaration = (ExternalDeclaration)layoutedInDeclarationTemplate.getInstanceFor(root, new ASTNode[]{newQualifier, data.type.cloneInto(root), member});
            newDeclarations.add(newDeclaration);
        }
        tree.injectNodes(ASTInjectionPoint.BEFORE_DECLARATIONS, newDeclarations);
    }

    static {
        layoutedOutDeclarationTemplate.markLocalReplacement(LayoutTransformer.layoutedOutDeclarationTemplate.getSourceRoot().nodeIndex.getOne(TypeQualifier.class));
        layoutedOutDeclarationTemplate.markLocalReplacement("__type", TypeSpecifier.class);
        layoutedOutDeclarationTemplate.markLocalReplacement("__name", DeclarationMember.class);
        layoutedInDeclarationTemplate.markLocalReplacement(LayoutTransformer.layoutedInDeclarationTemplate.getSourceRoot().nodeIndex.getOne(TypeQualifier.class));
        layoutedInDeclarationTemplate.markLocalReplacement("__type", TypeSpecifier.class);
        layoutedInDeclarationTemplate.markLocalReplacement("__name", DeclarationMember.class);
    }

    record NewDeclarationData(TypeQualifier qualifier, TypeSpecifier type, DeclarationMember member, int location, String name) {
    }

    private static class DeclarationMatcher
    extends Matcher<ExternalDeclaration> {
        private final StorageQualifier.StorageType storageType;

        public DeclarationMatcher(StorageQualifier.StorageType storageType) {
            super("out float name;", ParseShape.EXTERNAL_DECLARATION);
            this.markClassWildcard("qualifier", ((ExternalDeclaration)this.pattern).getRoot().nodeIndex.getUnique(TypeQualifier.class));
            this.markClassWildcard("type", ((ExternalDeclaration)this.pattern).getRoot().nodeIndex.getUnique(BuiltinNumericTypeSpecifier.class));
            this.markClassWildcard("name*", ((Identifier)((ExternalDeclaration)this.pattern).getRoot().identifierIndex.getUnique("name")).getAncestor(DeclarationMember.class));
            this.storageType = storageType;
        }

        public boolean matchesExtract(ExternalDeclaration tree) {
            boolean result = super.matchesExtract((ASTNode)tree);
            if (!result) {
                return false;
            }
            TypeQualifier qualifier = (TypeQualifier)this.getNodeMatch("qualifier", TypeQualifier.class);
            for (TypeQualifierPart part : qualifier.getParts()) {
                if (!(part instanceof StorageQualifier)) continue;
                StorageQualifier storageQualifier = (StorageQualifier)part;
                if (storageQualifier.storageType != this.storageType) continue;
                return true;
            }
            return false;
        }
    }
}

