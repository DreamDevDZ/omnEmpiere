package cm.itkamer.omnempiere.modelvalidator;

import org.adempiere.base.IModelValidatorFactory;
import org.compiere.model.ModelValidator;

public class ModelValidatorFactory implements IModelValidatorFactory {

	@Override
	public ModelValidator newModelValidatorInstance(String className) {
		if (className.equals(OmnEmpiereModelValidator.class.getName()))
			return new OmnEmpiereModelValidator();
		else
			return null;
	}
}
