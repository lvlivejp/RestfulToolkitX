package jiux.net.plugin.restful.navigator;


import com.intellij.icons.AllIcons;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.PsiClassImpl;
import com.intellij.ui.treeStructure.CachingSimpleNode;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.SimpleTreeBuilder;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.util.OpenSourceUtil;
import gnu.trove.THashMap;
import java.awt.event.InputEvent;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.Icon;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import jiux.net.plugin.restful.common.KtFunctionHelper;
import jiux.net.plugin.restful.common.PsiMethodHelper;
import jiux.net.plugin.restful.common.ToolkitIcons;
import jiux.net.plugin.restful.method.HttpMethod;
import jiux.net.plugin.restful.navigation.action.RestServiceItem;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtNamedFunction;

public class RestServiceStructure extends SimpleTreeStructure {
    public static final Logger LOG = Logger.getInstance(RestServiceStructure.class);
    private final Project myProject;
    private final RestServiceProjectsManager myProjectsManager;
    private final Map<RestServiceProject, ProjectNode> myProjectToNodeMapping = new THashMap<>();
    RestServiceDetail myRestServiceDetail;
    private SimpleTreeBuilder myTreeBuilder;
    private SimpleTree myTree;
    private RootNode myRoot = new RootNode();
    private int serviceCount = 0;

    public static Map<String, String> requestParamsMap = new HashMap<>();
    public static Map<String, String> requestHeaderMap = new HashMap<>();
    public static Map<String, String> requestBodyMap = new HashMap<>();


    public RestServiceStructure(Project project,
                                RestServiceProjectsManager projectsManager,
                                SimpleTree tree) {
        myProject = project;
        myProjectsManager = projectsManager;
        myTree = tree;
        myRestServiceDetail = project.getComponent(RestServiceDetail.class);

        configureTree(tree);

        myTreeBuilder = new SimpleTreeBuilder(tree, (DefaultTreeModel) tree.getModel(), this, null);
        Disposer.register(myProject, myTreeBuilder);

        myTreeBuilder.initRoot();
        myTreeBuilder.expand(myRoot, null);

    }

    public static <T extends BaseSimpleNode> List<T> getSelectedNodes(SimpleTree tree, Class<T> nodeClass) {
        final List<T> filtered = new ArrayList<>();
        for (SimpleNode node : getSelectedNodes(tree)) {
            if ((nodeClass != null) && (!nodeClass.isInstance(node))) {
                filtered.clear();
                break;
            }
            //noinspection unchecked
            filtered.add((T) node);
        }
        return filtered;
    }

    private static List<SimpleNode> getSelectedNodes(SimpleTree tree) {
        List<SimpleNode> nodes = new ArrayList<>();
        TreePath[] treePaths = tree.getSelectionPaths();
        if (treePaths != null) {
            for (TreePath treePath : treePaths) {
                nodes.add(tree.getNodeFor(treePath));
            }
        }
        return nodes;
    }

    private void configureTree(SimpleTree tree) {
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
    }

    @NotNull
    @Override
    public RootNode getRootElement() {
        return myRoot;
    }

    public void update() {
//        myTreeBuilder.setClearOnHideDelay(4);
//        myTreeBuilder.cleanUp();
//        myTreeBuilder.initRoot();
//        myTreeBuilder.expand(myRoot, null);
        // rest service controller
/*
        SimpleTree tree1 = new SimpleTree();
        myRoot = new RootNode();
        myTreeBuilder = new SimpleTreeBuilder(tree1, (DefaultTreeModel)tree1.getModel(), this, null);
        Disposer.register(myProject, myTreeBuilder);

        myTreeBuilder.initRoot();
        myTreeBuilder.expand(myRoot, null);*/
//        List<RestServiceProject> projects = myProjectsManager.getProjects();
//        List<RestServiceProject> projects = RestServiceProjectsManager.getInstance(myProject).getProjects();
/*        if (!ModalityState.current().equals(ModalityState.any()) && myProject.isInitialized()) {
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        }*/

        List<RestServiceProject> projects = RestServiceProjectsManager.getInstance(myProject).getServiceProjects();


//        Set<RestServiceProject> deleted = new HashSet<>(myProjectToNodeMapping.keySet());
//        deleted.removeAll(projects);
        updateProjects(projects);
    }

    public void updateProjects(List<RestServiceProject> projects) {
        serviceCount = 0;
//        DefaultMutableTreeNode rootTreeNode = createTreeNode("REST Services");
//        myTreeBuilder.addSubtreeToUpdate(rootTreeNode);

        for (RestServiceProject each : projects) {
            serviceCount += each.serviceItems.size();
            ProjectNode node = findNodeFor(each);
            if (node == null) {
                node = new ProjectNode(myRoot, each);
                myProjectToNodeMapping.put(each, node);
            }
        }
        myTreeBuilder.getUi().doUpdateFromRoot();
//        ((CachingSimpleNode) myRoot.getParent()).cleanUpCache();
//        myRoot.childrenChanged();
        myRoot.updateProjectNodes(projects);
    }

    private ProjectNode findNodeFor(RestServiceProject project) {
        return myProjectToNodeMapping.get(project);
    }

    public void updateFrom(SimpleNode node) {
        if (node == null) {
            return;
        }
        myTreeBuilder.addSubtreeToUpdateByElement(node);
    }

    private void updateUpTo(SimpleNode node) {
        if (node == null) {
            return;
        }
        SimpleNode each = node;
        while (each != null) {
            SimpleNode parent = each.getParent();
            /*if (parent != null) {
                ((BaseSimpleNode)parent).cleanUpCache();
            }*/
            updateFrom(each);
            each = each.getParent();
        }
    }

    private void resetRestServiceDetail() {
        myRestServiceDetail.resetRequestTabbedPane();
        myRestServiceDetail.setMethodValue(HttpMethod.GET.name());
        myRestServiceDetail.setUrlValue("URL");

        myRestServiceDetail.initTab();
    }

    public abstract class BaseSimpleNode extends CachingSimpleNode {

        protected BaseSimpleNode(SimpleNode aParent) {
            super(aParent);
        }

        @Nullable
        @NonNls
        String getActionId() {
            return null;
        }

        @Nullable
        @NonNls
        String getMenuId() {
            return null;
        }

        @Override
        public void cleanUpCache() {
            super.cleanUpCache();
        }

        protected void childrenChanged() {
            BaseSimpleNode each = this;
            while (each != null) {
                each.cleanUpCache();
                each = (BaseSimpleNode) each.getParent();
            }
            updateUpTo(this);
        }

    }

    public class RootNode extends BaseSimpleNode {
        List<ProjectNode> projectNodes = new ArrayList<>();

        protected RootNode() {
            super(null);
            //FIXME 由之前的 AllIcons.Actions.MODULE => AllIcons.Actions.ModuleDirectory
            getTemplatePresentation().setIcon(AllIcons.Actions.ModuleDirectory);
            setIcon(AllIcons.Actions.ModuleDirectory); //兼容 IDEA 2016
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return projectNodes.toArray(new SimpleNode[projectNodes.size()]);
        }

        @Override
        public String getName() {
            String s = "Found %d services ";// in {controllerCount} Controllers";
            return serviceCount > 0 ? String.format(s, serviceCount) : null;
        }

        @Override
        public void handleSelection(SimpleTree tree) {
//            System.out.println("ProjectNode handleSelection");
            resetRestServiceDetail();

        }

        public void updateProjectNodes(List<RestServiceProject> projects) {
//            cleanUpCache();
            projectNodes.clear();
            for (RestServiceProject project : projects) {
                ProjectNode projectNode = new ProjectNode(this, project);
                projectNodes.add(projectNode);
            }
            projectNodes = projectNodes.stream().sorted(Comparator.comparing(e->e.getName())).collect(Collectors.toList());

//                projectNode.updateServiceNodes();

            /*SimpleNode parent = getParent();
            if (parent != null) {
                ((BaseSimpleNode)parent).cleanUpCache();
            }*/
            updateFrom(getParent());
            childrenChanged();
//            updateUpTo(this);

        }

    }

    public class ProjectNode extends BaseSimpleNode {
        List<ControllerNode> controllerNodes = new ArrayList<>();
        RestServiceProject myProject;


        public ProjectNode(SimpleNode parent,/*,List<RestServiceItem> serviceItems*/RestServiceProject project) {
//            super(parent);
            super(parent);
            myProject = project;

            getTemplatePresentation().setIcon(ToolkitIcons.MODULE);
            setIcon(ToolkitIcons.MODULE); //兼容 IDEA 2016

            updateServiceNodes(project.serviceItems);
        }

        private void updateServiceNodes(List<RestServiceItem> serviceItems) {
            controllerNodes.clear();
            Map<String, List<RestServiceItem>> collect = serviceItems.stream().collect(Collectors.groupingBy(e -> ((PsiClassImpl) e.getPsiElement().getParent()).getName()));
            for (Map.Entry<String, List<RestServiceItem>> entry : collect.entrySet()) {
                controllerNodes.add(new ControllerNode(this, entry));
            }
            controllerNodes = controllerNodes.stream().sorted(Comparator.comparing(e->e.getName())).collect(Collectors.toList());
           /* for (int i = 0; i < 4; i++) {
                serviceNodes.add(new ServiceNode(this));
            }*/

            SimpleNode parent = getParent();
            if (parent != null) {
                ((BaseSimpleNode) parent).cleanUpCache();
            }
            updateFrom(parent);
//            childrenChanged();
//            updateUpTo(this);
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return controllerNodes.toArray(new SimpleNode[controllerNodes.size()]);
        }

        @Override
        public String getName() {
            return myProject.getModuleName();
        }


        @Override
        @Nullable
        @NonNls
        protected String getActionId() {
            return "Toolkit.RefreshServices";
        }
/*
        @Override
        @Nullable
        @NonNls
        protected String getMenuId() {
            return "Toolkit.ReimportServices";
        }*/

        @Override
        public void handleSelection(SimpleTree tree) {
//            System.out.println("ProjectNode handleSelection");
            resetRestServiceDetail();
        }

        @Override
        public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
//            System.out.println("ProjectNode handleDoubleClickOrEnter");
        }
    }

    public class ControllerNode extends BaseSimpleNode {
        List<ServiceNode> serviceNodes = new ArrayList<>();

        String controllerName=null;

        public ControllerNode(SimpleNode parent,Map.Entry<String, List<RestServiceItem>> entry) {
            super(parent);

            getTemplatePresentation().setIcon(ToolkitIcons.MODULE);
            setIcon(ToolkitIcons.MODULE); //兼容 IDEA 2016
            controllerName = entry.getKey();
            updateServiceNodes(entry.getValue());
        }

        private void updateServiceNodes(List<RestServiceItem> serviceItems) {
            serviceNodes.clear();
            for (RestServiceItem serviceItem : serviceItems) {
                serviceNodes.add(new ServiceNode(this, serviceItem));
            }
            serviceNodes = serviceNodes.stream().sorted(Comparator.comparing(e->e.getName())).collect(Collectors.toList());
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return serviceNodes.toArray(new SimpleNode[serviceNodes.size()]);
        }

        @Override
        public String getName() {
            return controllerName;
        }


        @Override
        @Nullable
        @NonNls
        protected String getActionId() {
            return "Toolkit.RefreshServices";
        }
/*
        @Override
        @Nullable
        @NonNls
        protected String getMenuId() {
            return "Toolkit.ReimportServices";
        }*/

        @Override
        public void handleSelection(SimpleTree tree) {
//            System.out.println("ProjectNode handleSelection");
            resetRestServiceDetail();
        }

        @Override
        public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
//            System.out.println("ProjectNode handleDoubleClickOrEnter");
        }
    }

    public class ServiceNode extends BaseSimpleNode {
        RestServiceItem myServiceItem;

        public ServiceNode(SimpleNode parent, RestServiceItem serviceItem) {
            super(parent);
            myServiceItem = serviceItem;

            Icon icon = ToolkitIcons.METHOD.get(serviceItem.getMethod());
            if (icon != null) {
                getTemplatePresentation().setIcon(icon);
                setIcon(icon); //兼容 IDEA 2016
            }
        }

        @Override
        protected SimpleNode[] buildChildren() {
            return new SimpleNode[0];
        }

        @Override
        public String getName() {
            String name = myServiceItem.getName();
/*            if (ToolkitIcons.METHOD.get(myServiceItem.getMethod()) == null && myServiceItem.getMethod() != null) {
                name += " [" + myServiceItem.getMethod() + "]";
            }*/
            return name;
        }

        @Override
        public void handleSelection(SimpleTree tree) {
            ServiceNode selectedNode = (ServiceNode) tree.getSelectedNode();
            assert selectedNode != null;
            showServiceDetail(selectedNode.myServiceItem);
        }


        /**
         * 显示服务详情，url
         */
        private void showServiceDetail(RestServiceItem serviceItem) {

            myRestServiceDetail.resetRequestTabbedPane();

            String method = serviceItem.getMethod() != null ? String.valueOf(serviceItem.getMethod()) : HttpMethod.GET.name();
            myRestServiceDetail.setMethodValue(method);
            myRestServiceDetail.setUrlValue(serviceItem.getFullUrl());


            String requestParams = "";
            String requestBodyJson = "";
            PsiElement psiElement = serviceItem.getPsiElement();
            if (psiElement.getLanguage() == JavaLanguage.INSTANCE) {
                PsiMethodHelper psiMethodHelper = PsiMethodHelper.create(serviceItem.getPsiMethod()).withModule(serviceItem.getModule());
                requestParams = psiMethodHelper.buildParamString();
                requestBodyJson = psiMethodHelper.buildRequestBodyJson();

            } else if (psiElement.getLanguage() == KotlinLanguage.INSTANCE) {
                if (psiElement instanceof KtNamedFunction) {
                    KtNamedFunction ktNamedFunction = (KtNamedFunction) psiElement;
                    KtFunctionHelper ktFunctionHelper = (KtFunctionHelper) KtFunctionHelper.create(ktNamedFunction).withModule(serviceItem.getModule());
                    requestParams = ktFunctionHelper.buildParamString();
                    requestBodyJson = ktFunctionHelper.buildRequestBodyJson();
                }

            }
            String text = requestParamsMap.get(method + "_" + serviceItem.getFullUrl());
            if (StringUtils.isNotBlank(text)) {
                requestParams = text;
            }
            String headerText = requestHeaderMap.get(method + "_" + serviceItem.getFullUrl());

            myRestServiceDetail.addRequestParamsTab(requestParams,headerText);

            String requestBody = requestBodyMap.get(method + "_" + serviceItem.getFullUrl());
            if (StringUtils.isNotBlank(requestBody)) {
                requestBodyJson = requestBody;
            }
            if (StringUtils.isNotBlank(requestBodyJson)) {
                myRestServiceDetail.addRequestBodyTabPanel(requestBodyJson);
            }
        }

        @Override
        public void handleDoubleClickOrEnter(SimpleTree tree, InputEvent inputEvent) {
            try {
                ServiceNode selectedNode = (ServiceNode) tree.getSelectedNode();

                RestServiceItem myServiceItem = selectedNode.myServiceItem;
                PsiElement psiElement = myServiceItem.getPsiElement();

                if (!psiElement.isValid()) {
                    // PsiDocumentManager.getInstance(psiMethod.getProject()).commitAllDocuments();
                    // try refresh service
                    LOG.info("psiMethod is invalid: ");
                    LOG.info(psiElement.toString());
                    RestServicesNavigator.getInstance(myServiceItem.getModule().getProject()).scheduleStructureUpdate();
                }

                if (psiElement.getLanguage() == JavaLanguage.INSTANCE) {
                    PsiMethod psiMethod = myServiceItem.getPsiMethod();
                    OpenSourceUtil.navigate(psiMethod);

                } else if (psiElement.getLanguage() == KotlinLanguage.INSTANCE) {
                    if (psiElement instanceof KtNamedFunction) {
                        KtNamedFunction ktNamedFunction = (KtNamedFunction) psiElement;
                        OpenSourceUtil.navigate(ktNamedFunction);
                    }
                }
            } catch (ClassCastException ignore) {
                // ServiceNode cast ignored.
                // java.lang.ClassCastException: class jiux.net.plugin.restful.navigator.RestServiceStructure$ProjectNode
                // cannot be cast to class jiux.net.plugin.restful.navigator.RestServiceStructure$ServiceNode
                // (jiux.net.plugin.restful.navigator.RestServiceStructure$ProjectNode
                // and jiux.net.plugin.restful.navigator.RestServiceStructure$ServiceNode
                // are in unnamed module of loader com.intellij.ide.plugins.cl.PluginClassLoader @742ad77c)
            }
        }

        @Override
        @Nullable
        @NonNls
        protected String getMenuId() {
            return "Toolkit.NavigatorServiceMenu";
        }

/*
        @Override
        @Nullable
        @NonNls
        protected String getActionId() {
            return "Toolkit.Navigator";
        }*/

    }

}
