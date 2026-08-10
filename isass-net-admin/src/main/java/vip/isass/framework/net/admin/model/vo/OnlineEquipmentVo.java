// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.admin.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineEquipmentVo {

    private String equipmentId;

    private String userId;

}
