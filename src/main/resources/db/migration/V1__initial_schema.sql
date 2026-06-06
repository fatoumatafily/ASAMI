CREATE TABLE sellers (
    id UUID PRIMARY KEY,
    business_name VARCHAR(255) NOT NULL,
    whatsapp_phone_number_id VARCHAR(255) NOT NULL UNIQUE,
    default_language VARCHAR(10) NOT NULL DEFAULT 'fr',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL REFERENCES sellers(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'XOF',
    stock_quantity INTEGER CHECK (stock_quantity IS NULL OR stock_quantity >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_seller_active ON products(seller_id, active);

CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    seller_id UUID NOT NULL REFERENCES sellers(id) ON DELETE CASCADE,
    customer_phone VARCHAR(30) NOT NULL,
    preferred_language VARCHAR(10),
    status VARCHAR(30) NOT NULL DEFAULT 'BOT_ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (seller_id, customer_phone)
);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    whatsapp_message_id VARCHAR(255) UNIQUE,
    direction VARCHAR(10) NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    text_content TEXT,
    media_id VARCHAR(255),
    detected_language VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_messages_conversation_created
    ON messages(conversation_id, created_at);
