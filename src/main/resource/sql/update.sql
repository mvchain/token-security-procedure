SET FOREIGN_KEY_CHECKS=0;

ALTER TABLE `account` MODIFY COLUMN `type`  int(1) NULL DEFAULT NULL COMMENT '1ETH 2BTC' AFTER `id`;
ALTER TABLE `account` ROW_FORMAT=Compact;
ALTER TABLE `mission` ROW_FORMAT=Compact;
ALTER TABLE `orders` MODIFY COLUMN `order_id`  longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL AFTER `id`;
ALTER TABLE `orders` MODIFY COLUMN `to_address`  longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL AFTER `value`;
ALTER TABLE `orders` MODIFY COLUMN `signature`  longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL AFTER `mission_id`;
ALTER TABLE `orders` MODIFY COLUMN `from_address`  longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL AFTER `signature`;
ALTER TABLE `orders` ADD COLUMN `fee`  decimal(30,10) NULL DEFAULT NULL AFTER `nonce`;
ALTER TABLE `orders` ROW_FORMAT=Compact;
SET FOREIGN_KEY_CHECKS=1;