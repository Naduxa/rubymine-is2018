package com.jetbrains.python.inspection;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static com.jetbrains.python.PyTokenTypes.*;

public class PyConstantExpression extends PyInspection {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly,
                                          @NotNull LocalInspectionToolSession session) {
        return new Visitor(holder, session);
    }

    private static class Visitor extends PyInspectionVisitor {

        private static List<PyElementType> listCompareOperators = Arrays.asList(LT, GT, LE, GE, EQEQ, NE, NE_OLD);
        private Visitor(@Nullable ProblemsHolder holder, @NotNull LocalInspectionToolSession session) {
            super(holder, session);
        }

        @Override
        public void visitPyIfStatement(PyIfStatement node) {
            super.visitPyIfStatement(node);
            processIfPart(node.getIfPart());
            for (PyIfPart part : node.getElifParts()) {
                processIfPart(part);
            }
        }

        private Boolean getComparableExpressionValue(@NotNull PyNumericLiteralExpression first, @NotNull PyNumericLiteralExpression second, @NotNull PyElementType operator){
            final BigDecimal a = first.getBigDecimalValue();
            final BigDecimal b = second.getBigDecimalValue();
            if (operator.equals(LT)){
                return a.compareTo(b) < 0;
            }
            if (operator.equals(GT)){
                return a.compareTo(b) > 0;
            }
            if (operator.equals(LE)){
                return a.compareTo(b) <= 0;
            }
            if (operator.equals(GE)){
                return a.compareTo(b) >= 0;
            }
            if (operator.equals(EQEQ)){
                return a.compareTo(b) == 0;
            }

            return a.compareTo(b) != 0;

        }

        private boolean isComparisonExpression(@NotNull PyExpression left, @NotNull PyExpression right, @NotNull PyElementType operator){
            return (listCompareOperators.contains(operator) && left instanceof PyNumericLiteralExpression
                    && right instanceof PyNumericLiteralExpression);
        }

        private void processExpression(PyExpression expression){
            if (expression == null)
                return;

            if (expression instanceof PyParenthesizedExpression){
                processExpression(((PyParenthesizedExpression) expression).getContainedExpression());
                return;
            }
            if (!(expression instanceof PyBinaryExpression)){
                return;
            }
            final PyElementType operator = ((PyBinaryExpression)expression).getOperator();
            final PyExpression left = ((PyBinaryExpression)expression).getLeftExpression();
            final PyExpression right = ((PyBinaryExpression)expression).getRightExpression();

            if (left != null && right != null && operator != null
                    && isComparisonExpression(left, right, operator)){
                registerProblem(expression, "The condition is always " +
                        getComparableExpressionValue((PyNumericLiteralExpression)left, (PyNumericLiteralExpression)right, operator).toString());
                return;
            }
            processExpression(left);
            processExpression(right);
        }

        private void processIfPart(@NotNull PyIfPart pyIfPart) {
            final PyExpression condition = pyIfPart.getCondition();
            if (condition instanceof PyBoolLiteralExpression) {
                registerProblem(condition, "The condition is always " + ((PyBoolLiteralExpression) condition).getValue());
                return;
            }
            processExpression(condition);
        }
    }
}
