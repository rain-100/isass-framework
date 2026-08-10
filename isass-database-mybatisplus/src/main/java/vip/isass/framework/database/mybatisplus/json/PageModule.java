// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.database.mybatisplus.json;

import tools.jackson.databind.module.SimpleModule;

public class PageModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        super.setupModule(context);
        context.addValueInstantiators(new PageValueInstantiators());
    }

}
