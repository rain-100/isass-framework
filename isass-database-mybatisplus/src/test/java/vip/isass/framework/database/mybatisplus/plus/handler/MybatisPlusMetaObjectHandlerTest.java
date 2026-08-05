package vip.isass.framework.database.mybatisplus.plus.handler;

import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;
import vip.isass.framework.nocode.entity.ILogicDeleteEntity;
import vip.isass.framework.nocode.entity.ITenantEntity;
import vip.isass.framework.nocode.entity.ITraceEntity;
import vip.isass.framework.nocode.entity.IVersionEntity;

import static org.assertj.core.api.Assertions.assertThat;

class MybatisPlusMetaObjectHandlerTest {

    @Test
    void fillsCommonFieldsThroughEntityInterfaces() {
        FillEntity entity = new FillEntity();

        new MybatisPlusMetaObjectHandler().insertFill(SystemMetaObject.forObject(entity));

        assertThat(entity.getVersion()).isEqualTo(IVersionEntity.DEFAULT_VERSION);
        assertThat(entity.getTenantId()).isZero();
        assertThat(entity.getAppId()).isZero();
        assertThat(entity.getCreateUserId()).isZero();
        assertThat(entity.getModifyUserId()).isZero();
        assertThat(entity.getCreateTime()).isNotNull();
        assertThat(entity.getModifyTime()).isNotNull();
        assertThat(entity.getDeleteFlag()).isFalse();
    }

    private static final class FillEntity implements IVersionEntity<FillEntity>, ITenantEntity<Long, FillEntity>,
            ITraceEntity<Long, FillEntity>, ILogicDeleteEntity<FillEntity> {

        private Integer version;
        private Long tenantId;
        private Long appId;
        private Long createUserId;
        private String createUserName;
        private Long createTime;
        private Long modifyUserId;
        private String modifyUserName;
        private Long modifyTime;
        private Boolean deleteFlag;

        @Override
        public Integer getVersion() {
            return version;
        }

        @Override
        public void setVersion(Integer version) {
            this.version = version;
        }

        @Override
        public Long getTenantId() {
            return tenantId;
        }

        @Override
        public void setTenantId(Long tenantId) {
            this.tenantId = tenantId;
        }

        public Long getAppId() {
            return appId;
        }

        public void setAppId(Long appId) {
            this.appId = appId;
        }

        @Override
        public Long getCreateUserId() {
            return createUserId;
        }

        @Override
        public void setCreateUserId(Long createUserId) {
            this.createUserId = createUserId;
        }

        @Override
        public String getCreateUserName() {
            return createUserName;
        }

        @Override
        public void setCreateUserName(String createUserName) {
            this.createUserName = createUserName;
        }

        @Override
        public Long getCreateTime() {
            return createTime;
        }

        @Override
        public void setCreateTime(Long createTime) {
            this.createTime = createTime;
        }

        @Override
        public Long getModifyUserId() {
            return modifyUserId;
        }

        @Override
        public void setModifyUserId(Long modifyUserId) {
            this.modifyUserId = modifyUserId;
        }

        @Override
        public String getModifyUserName() {
            return modifyUserName;
        }

        @Override
        public void setModifyUserName(String modifyUserName) {
            this.modifyUserName = modifyUserName;
        }

        @Override
        public Long getModifyTime() {
            return modifyTime;
        }

        @Override
        public void setModifyTime(Long modifyTime) {
            this.modifyTime = modifyTime;
        }

        @Override
        public Boolean getDeleteFlag() {
            return deleteFlag;
        }

        @Override
        public void setDeleteFlag(Boolean deleteFlag) {
            this.deleteFlag = deleteFlag;
        }

        @Override
        public FillEntity randomEntity() {
            return this;
        }
    }
}
