package jiux.net.plugin.restful.icon;

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.PsiClassImpl;
import jiux.net.plugin.restful.navigator.RestServicesNavigator;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class HaloLineMarker implements LineMarkerProvider {
    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement psiElement) {
        LineMarkerInfo lineMarkerInfo= null;
        try {
            lineMarkerInfo = null;
            String annos="org.springframework.web.bind.annotation.GetMapping,org.springframework.web.bind.annotation.PostMapping";
            String anno = judgeHaveAnnotation(psiElement, annos);
            if(StringUtils.isBlank(anno)){
                return lineMarkerInfo;
            }
            PsiMethod field = ((PsiMethod) psiElement);
            PsiAnnotation psiAnnotation = field.getAnnotation(anno);
            String controllerName = ((PsiClassImpl) field.getParent()).getName();
            String projectName = field.getProject().getName();
            PsiAnnotation annotation = ((PsiClassImpl) field.getParent()).getAnnotation("org.springframework.web.bind.annotation.RequestMapping");
            String requestMapping = annotation.getText();
            lineMarkerInfo = new LineMarkerInfo<>(psiAnnotation, psiAnnotation.getTextRange(), IconLoader.findIcon("/icons/service.png"),
                    psiAnnotationFunc -> "jump to restfulTool",
                    (GutterIconNavigationHandler) (e, elt) -> {
                        String url = elt.getText();
                        url = requestMapping + url;
                        RestServicesNavigator.getInstance(elt.getProject()).show(projectName,controllerName,url);
                    },// ➊
                    GutterIconRenderer.Alignment.LEFT);
        } catch (Exception e) {
            e.printStackTrace(); // ➋
        }
        return lineMarkerInfo;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        LineMarkerProvider.super.collectSlowLineMarkers(elements, result);
    }

    private String judgeHaveAnnotation(@NotNull PsiElement psiElement, String annos) {
        if (psiElement instanceof PsiMethod) {
            PsiMethod field = ((PsiMethod) psiElement);
            for (String anno : annos.split(",")) {
                PsiAnnotation psiAnnotation = field.getAnnotation(anno);
                if (null != psiAnnotation) {
                    return anno;
                }
            }
        }
        return null;
    }
}
