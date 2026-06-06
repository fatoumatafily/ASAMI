ALTER TABLE sellers ALTER COLUMN whatsapp_phone_number_id DROP NOT NULL;
ALTER TABLE sellers ADD COLUMN whatsapp_business_account_id VARCHAR(255);
ALTER TABLE sellers ADD COLUMN whatsapp_display_phone VARCHAR(30);
ALTER TABLE sellers ADD COLUMN whatsapp_connection_status VARCHAR(30) NOT NULL DEFAULT 'DISCONNECTED';

CREATE TABLE seller_accounts (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL UNIQUE REFERENCES sellers(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
