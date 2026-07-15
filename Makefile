
graphify-install:
	uv tool install graphifyy

graphify-init:
	graphify . --code-only && graphify cluster-only .

graphify-opencode:
	graphify opencode install