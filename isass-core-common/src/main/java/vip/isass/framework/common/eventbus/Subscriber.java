// SPDX-License-Identifier: LGPL-3.0-only

// package vip.isass.framework.eventbus;
//
// import jakarta.annotation.PostConstruct;
// import jakarta.annotation.PreDestroy;
//
// /**
//  * @author Rain
//  */
// public interface Subscriber {
//
//     BlueEventBus getBlueEventBus();
//
//     @PostConstruct
//     default void register() {
//         BlueEventBus eventBus = getBlueEventBus();
//         if (eventBus == null) {
//             return;
//         }
//         eventBus.register(this);
//     }
//
//     @PreDestroy
//     default void unregister() {
//         BlueEventBus eventBus = getBlueEventBus();
//         if (eventBus == null) {
//             return;
//         }
//         eventBus.unregister(this);
//     }
//
// }
