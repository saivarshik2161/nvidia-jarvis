"""Initial migration

Revision ID: 001
Revises: 
Create Date: 2026-09-25 16:37:00.000000

"""
from alembic import op
import sqlalchemy as sa
import uuid


# revision identifiers, used by Alembic.
revision = '001'
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.create_table(
        'memories',
        sa.Column('id', sa.String(36), primary_key=True, default=lambda: str(uuid.uuid4())),
        sa.Column('device_id', sa.String(255), nullable=False, index=True),
        sa.Column('content', sa.Text, nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column('updated_at', sa.DateTime(timezone=True), server_default=sa.func.now(), onupdate=sa.func.now(), nullable=False),
    )
    op.create_index('ix_memories_device_id_created_at', 'memories', ['device_id', 'created_at'])


def downgrade() -> None:
    op.drop_index('ix_memories_device_id_created_at', table_name='memories')
    op.drop_table('memories')